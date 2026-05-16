package org.search.search.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.search.search.entity.DictionaryStore;
import org.search.search.mapper.DictionaryStoreMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 词典管理后台接口
 */
@RestController
@RequestMapping("/api/search/admin/dict")
@RequiredArgsConstructor
public class DictionaryAdminController {

    private final DictionaryStoreMapper dictionaryStoreMapper;
    private final StringRedisTemplate redisTemplate;

    // 拆分后的 Redis Key，与 Service 层保持一致
    private static final String DICT_HOTWORD_LAST_MODIFIED_KEY = "dict:hotword:last:modified";
    private static final String DICT_SYNONYM_LAST_MODIFIED_KEY = "dict:synonym:last:modified";
    private static final String DICT_MERGE_CONFIG_KEY = "dict:config:merge_synonyms";

    /**
     * 获取智能合并开关状态
     */
    @GetMapping("/config/merge")
    public Map<String, Object> getMergeConfig() {
        String val = redisTemplate.opsForValue().get(DICT_MERGE_CONFIG_KEY);
        // 默认 true
        boolean enabled = val == null || "true".equals(val);
        return Map.of("code", 200, "enabled", enabled);
    }

    /**
     * 设置智能合并开关
     */
    @PostMapping("/config/merge")
    public Map<String, Object> setMergeConfig(@RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) enabled = true;
        
        //  redisTemplate.opsForValue().set(DICT_MERGE_CONFIG_KEY, String.valueOf(enabled));
        
        // 开关变了，同义词文件内容也会变，必须强制刷新时间戳，触发重新生成
        long now = System.currentTimeMillis();
       // redisTemplate.opsForValue().set(DICT_SYNONYM_LAST_MODIFIED_KEY, String.valueOf(now));
        
        return Map.of("code", 200, "message", "设置成功，正在重新生成词典...");
    }

    /**
     * 分页查询词典内容
     */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    @RequestParam(required = false) String word) {
        Page<DictionaryStore> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DictionaryStore> wrapper = new LambdaQueryWrapper<>();
        
        // 如果提供了搜索词，执行模糊查询
        if (word != null && !word.isEmpty()) {
            wrapper.like(DictionaryStore::getWord, word);
        }
        wrapper.orderByDesc(DictionaryStore::getCreateTime);

        Page<DictionaryStore> result = dictionaryStoreMapper.selectPage(pageParam, wrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("data", result);
        return map;
    }

    /**
     * 手动新增词汇
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody DictionaryStore dict) {
        // 1. 查重：避免重复添加相同的核心词汇
        Long count = dictionaryStoreMapper.selectCount(new LambdaQueryWrapper<DictionaryStore>()
                .eq(DictionaryStore::getWord, dict.getWord()));
        if (count > 0) {
            return Map.of("code", 400, "message", "该词汇已存在于词典中");
        }

        // 2. 设置默认值并入库
        dict.setCreateTime(LocalDateTime.now());
        if (dict.getIsEnabled() == null) dict.setIsEnabled(true);
        if (dict.getType() == null) dict.setType("other");

        dictionaryStoreMapper.insert(dict);
        
        // 3. 刷新时间戳，触发热更新
        refreshLastModified(dict);

        return Map.of("code", 200, "message", "添加成功");
    }

    /**
     * 更新词条信息
     */
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody DictionaryStore dict) {
        if (dict.getId() == null) {
            return Map.of("code", 400, "message", "更新失败：缺少词项ID");
        }
        
        dictionaryStoreMapper.updateById(dict);
        
        // 刷新时间戳
        refreshLastModified(dict);
        
        return Map.of("code", 200, "message", "更新成功");
    }

    /**
     * 删除词项
     */
    @PostMapping("/delete")
    public Map<String, Object> delete(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        if (id == null) {
            return Map.of("code", 400, "message", "删除失败：未指定ID");
        }

        // 查找原始数据，确定需要更新哪个时间戳
        DictionaryStore exist = dictionaryStoreMapper.selectById(id);
        if (exist != null) {
            dictionaryStoreMapper.deleteById(id);
            // 删除后也要通知搜索引擎
            refreshLastModified(exist);
        }
        
        return Map.of("code", 200, "message", "删除成功");
    }

    /**
     * 刷新词典更新时间戳，通过 Redis 通知分词器和搜索引擎
     * 逻辑：解耦热词和同义词，谁变了刷谁
     */
    private void refreshLastModified(DictionaryStore dict) {
        long now = System.currentTimeMillis();
        
        // 1. 核心词汇变动，必定影响分词 (热词)
        redisTemplate.opsForValue().set(DICT_HOTWORD_LAST_MODIFIED_KEY, String.valueOf(now));
        
        // 2. 如果该记录包含同义词规则，则同步更新同义词的时间戳
        if (dict.getSynonymRule() != null && !dict.getSynonymRule().trim().isEmpty()) {
            redisTemplate.opsForValue().set(DICT_SYNONYM_LAST_MODIFIED_KEY, String.valueOf(now));
        }
    }
}