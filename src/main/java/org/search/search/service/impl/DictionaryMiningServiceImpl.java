package org.search.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.search.search.entity.DictionaryStore;
import org.search.search.entity.SearchTermTask;
import org.search.search.mapper.DictionaryStoreMapper;
import org.search.search.mapper.SearchTermTaskMapper;
import org.search.search.service.DeepSeekService;
import org.search.search.service.DictionaryMiningService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 词典挖掘服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryMiningServiceImpl implements DictionaryMiningService {

    private final SearchTermTaskMapper searchTermTaskMapper;
    private final DictionaryStoreMapper dictionaryStoreMapper;
    private final StringRedisTemplate redisTemplate;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper;

    private static final String SEARCH_HOT_WORD_KEY = "search:hot:words";
    // 拆分后的 Redis Key
    private static final String DICT_HOTWORD_LAST_MODIFIED_KEY = "dict:hotword:last:modified";
    private static final String DICT_SYNONYM_LAST_MODIFIED_KEY = "dict:synonym:last:modified";

    // 记录上一次真正同步到本地文件的时间 (内存标记)
    private long lastHotWordSyncTimestamp = 0L;
    private long lastSynonymSyncTimestamp = 0L;

    @Override
    public long getLastModifiedTime() {
        // 这是一个兜底方法，为了兼容旧接口，取两者的最大值
        long t1 = getLastHotWordModifiedTime();
        long t2 = getLastSynonymModifiedTime();
        return Math.max(t1, t2);
    }
    
    @Override
    public long getLastHotWordModifiedTime() {
        String timeStr = redisTemplate.opsForValue().get(DICT_HOTWORD_LAST_MODIFIED_KEY);
        return timeStr != null ? Long.parseLong(timeStr) : System.currentTimeMillis();
    }

    @Override
    public long getLastSynonymModifiedTime() {
        String timeStr = redisTemplate.opsForValue().get(DICT_SYNONYM_LAST_MODIFIED_KEY);
        return timeStr != null ? Long.parseLong(timeStr) : System.currentTimeMillis();
    }

    /**
     * 极速记录用户搜索词 (Redis 原子操作)
     */
    @Override
    public void recordSearchTerm(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        String term = keyword.trim();
        redisTemplate.opsForZSet().incrementScore(SEARCH_HOT_WORD_KEY, term, 1);
    }

    /**
     * 定时任务1：每分钟将 Redis 中的热词同步到 MySQL (收集阶段)
     */
    @Override
    @Scheduled(fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void syncToDatabase() {
        log.info("开始从 Redis 同步搜索词到数据库...");

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .rangeWithScores(SEARCH_HOT_WORD_KEY, 0, -1);

        if (tuples == null || tuples.isEmpty()) {
            return;
        }

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String term = tuple.getValue();
            Double score = tuple.getScore();
            int increment = (score != null) ? score.intValue() : 0;

            if (term == null || increment == 0) continue;

            try {
                processSingleTerm(term, increment);
                redisTemplate.opsForZSet().remove(SEARCH_HOT_WORD_KEY, term);
            } catch (Exception e) {
                log.error("同步词条失败: {}", term, e);
            }
        }
        log.info("已同步 {} 个词条到数据库。", tuples.size());
    }

    /**
     * 定时任务2：每5分钟调用 AI 分析待处理的搜索词 (分析阶段)
     */
    @Override
    @Scheduled(fixedRate = 300000)
    public void mineTermsFromAi() {
        log.info("开始执行 AI 挖掘任务...");

        List<SearchTermTask> pendingTasks = searchTermTaskMapper.selectList(
                new LambdaQueryWrapper<SearchTermTask>()
                        .eq(SearchTermTask::getStatus, 0)
                        .orderByDesc(SearchTermTask::getHitCount)
                        .last("LIMIT 50")
        );

        if (pendingTasks.isEmpty()) {
            log.info("暂无待分析的搜索词。");
            return;
        }

        List<String> termsToAnalyze = pendingTasks.stream()
                .map(SearchTermTask::getTerm)
                .collect(Collectors.toList());

        String jsonResponse = deepSeekService.analyzeTerms(termsToAnalyze);
        if (jsonResponse == null) return;

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode results = root.path("results");

            if (results.isArray()) {
                boolean hasUpdates = false;
                for (JsonNode node : results) {
                    String word = node.path("word").asText();
                    boolean isValid = node.path("is_valid").asBoolean();

                    SearchTermTask task = pendingTasks.stream()
                            .filter(t -> t.getTerm().equals(word))
                            .findFirst()
                            .orElse(null);
                    
                    if (task == null) continue;

                    if (isValid) {
                        String synonymRule = node.path("synonym_rule").asText(null);
                        String type = node.path("type").asText("history");

                        if (dictionaryStoreMapper.selectCount(new LambdaQueryWrapper<DictionaryStore>().eq(DictionaryStore::getWord, word)) == 0) {
                            DictionaryStore dict = new DictionaryStore();
                            dict.setWord(word);
                            dict.setSynonymRule(synonymRule);
                            dict.setType(type);
                            dict.setIsEnabled(true);
                            dict.setCreateTime(LocalDateTime.now());
                            dictionaryStoreMapper.insert(dict);
                            hasUpdates = true;
                        }
                        task.setStatus(1);
                    } else {
                        task.setStatus(2);
                    }
                    task.setUpdateTime(LocalDateTime.now());
                    searchTermTaskMapper.updateById(task);
                }
                
                if (hasUpdates) {
                    // AI 挖掘通常主要是加新词，但也可能包含同义词规则
                    // 简单起见，这里两个都更新
                    long now = System.currentTimeMillis();
                    redisTemplate.opsForValue().set(DICT_HOTWORD_LAST_MODIFIED_KEY, String.valueOf(now));
                    redisTemplate.opsForValue().set(DICT_SYNONYM_LAST_MODIFIED_KEY, String.valueOf(now));
                    
                    // 触发一次文件同步，让本地 IK 尽快感知
                    syncFilesToLocal();
                }
                
                log.info("AI 分析完成，共处理 {} 个词条。", results.size());
            }

        } catch (Exception e) {
            log.error("处理 AI 响应时出错", e);
        }
    }

    /**
     * 定时任务3：同步词典到本地文件 (供 IK 和 ES 热更新)
     */
    @Override
    @Scheduled(fixedRate = 60000)
    public void syncFilesToLocal() {
        // 1. 检查热词更新
        long remoteHotWordTime = getLastHotWordModifiedTime();
        if (remoteHotWordTime > lastHotWordSyncTimestamp) {
            log.info("检测到热词发生变化，同步 hot-words.dic ...");
            String hotWordsContent = getHotWordsDictionary();
            if (!hotWordsContent.isEmpty()) {
                writeToFile("hot-words.dic", hotWordsContent);
                this.lastHotWordSyncTimestamp = remoteHotWordTime;
            }
        }

        // 2. 检查同义词更新
        long remoteSynonymTime = getLastSynonymModifiedTime();
        if (remoteSynonymTime > lastSynonymSyncTimestamp) {
            log.info("检测到同义词发生变化，同步 synonyms.txt ...");
            String synonymsContent = getSynonymsDictionary();
            if (!synonymsContent.isEmpty()) {
                writeToFile("synonyms.txt", synonymsContent);
                this.lastSynonymSyncTimestamp = remoteSynonymTime;
            }
        }
    }

    private void writeToFile(String filename, String content) {
        try {
            String userDir = System.getProperty("user.dir");
            
            // 目标路径列表
            List<String> paths = List.of(
                userDir + "/elasticsearch-install/elasticsearch-6.8.23/config/analysis-ik/" + filename,
                userDir + "/elasticsearch-install/elasticsearch-6.8.23/plugins/analysis-ik/config/" + filename,
                userDir + "/plugins/elasticsearch-analysis-ik-6.8.23/config/" + filename
            );

            boolean written = false;
            for (String pathStr : paths) {
                File file = new File(pathStr);
                
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                
                if (file.getParentFile().exists()) {
                    Files.writeString(file.toPath(), content, StandardCharsets.UTF_8, 
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    written = true;
                }
            }
            if (written) {
                log.info("已成功同步 {} 到本地文件。", filename);
            }
        } catch (Exception e) {
            log.error("同步文件失败: " + filename, e);
        }
    }

    private void processSingleTerm(String term, int countIncrement) {
        Long dictCount = dictionaryStoreMapper.selectCount(
                new LambdaQueryWrapper<DictionaryStore>().eq(DictionaryStore::getWord, term)
        );
        if (dictCount > 0) return;

        SearchTermTask task = searchTermTaskMapper.selectOne(
                new LambdaQueryWrapper<SearchTermTask>().eq(SearchTermTask::getTerm, term)
        );

        if (task != null) {
            if (task.getStatus() == 0) {
                task.setHitCount(task.getHitCount() + countIncrement);
                task.setUpdateTime(LocalDateTime.now());
                searchTermTaskMapper.updateById(task);
            }
        } else {
            SearchTermTask newTask = new SearchTermTask();
            newTask.setTerm(term);
            newTask.setHitCount(countIncrement);
            newTask.setStatus(0);
            newTask.setCreateTime(LocalDateTime.now());
            newTask.setUpdateTime(LocalDateTime.now());
            searchTermTaskMapper.insert(newTask);
        }
    }

    @Override
    public String getHotWordsDictionary() {
        List<DictionaryStore> list = dictionaryStoreMapper.selectList(
                new LambdaQueryWrapper<DictionaryStore>()
                        .eq(DictionaryStore::getIsEnabled, true)
        );
        StringBuilder sb = new StringBuilder();
        for (DictionaryStore item : list) {
            if (item.getWord() != null && !item.getWord().isEmpty()) {
                sb.append(item.getWord()).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String getSynonymsDictionary() {
        List<DictionaryStore> list = dictionaryStoreMapper.selectList(
                new LambdaQueryWrapper<DictionaryStore>()
                        .eq(DictionaryStore::getIsEnabled, true)
                        .isNotNull(DictionaryStore::getSynonymRule)
        );
        
        // 检查全局合并开关 (从 Redis 读取，默认开启)
        String mergeConfig = redisTemplate.opsForValue().get("dict:config:merge_synonyms");
        boolean enableMerge = mergeConfig == null || "true".equals(mergeConfig);

        if (!enableMerge) {
            // 原始模式：直接输出
            StringBuilder sb = new StringBuilder();
            for (DictionaryStore item : list) {
                String rule = item.getSynonymRule();
                if (rule != null && !rule.trim().isEmpty()) {
                    sb.append(rule.trim()).append("\n");
                }
            }
            return sb.toString();
        }

        // 智能合并模式：基于并查集/图的连通分量算法
        return buildMergedSynonyms(list);
    }

    /**
     * 智能合并同义词规则
     * 输入: [A,B], [B,C]
     * 输出: A,B,C
     */
    private String buildMergedSynonyms(List<DictionaryStore> list) {
        // 1. 构建邻接表
        java.util.Map<String, Set<String>> graph = new java.util.HashMap<>();
        for (DictionaryStore item : list) {
            String rule = item.getSynonymRule();
            if (rule == null || rule.trim().isEmpty()) continue;
            
            // 只处理逗号分隔的等价规则，跳过 "=>" 映射规则
            if (rule.contains("=>")) {
                // 映射规则无法简单合并，直接保留原样（暂不参与图构建，或单独处理）
                continue; 
            }

            String[] words = rule.split("[,，]");
            for (int i = 0; i < words.length; i++) {
                String w1 = words[i].trim();
                for (int j = i + 1; j < words.length; j++) {
                    String w2 = words[j].trim();
                    if (!w1.isEmpty() && !w2.isEmpty()) {
                        graph.computeIfAbsent(w1, k -> new java.util.HashSet<>()).add(w2);
                        graph.computeIfAbsent(w2, k -> new java.util.HashSet<>()).add(w1);
                    }
                }
            }
        }

        // 2. 寻找连通分量 (BFS)
        StringBuilder sb = new StringBuilder();
        Set<String> visited = new java.util.HashSet<>();
        
        // 先把刚才跳过的 "=>" 规则原文加进去
        for (DictionaryStore item : list) {
            if (item.getSynonymRule() != null && item.getSynonymRule().contains("=>")) {
                sb.append(item.getSynonymRule().trim()).append("\n");
            }
        }

        for (String node : graph.keySet()) {
            if (visited.contains(node)) continue;

            Set<String> component = new java.util.LinkedHashSet<>(); // 保持顺序纯粹为了美观
            java.util.Queue<String> queue = new java.util.LinkedList<>();
            queue.add(node);
            visited.add(node);
            component.add(node);

            while (!queue.isEmpty()) {
                String current = queue.poll();
                for (String neighbor : graph.getOrDefault(current, java.util.Collections.emptySet())) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        component.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }

            // 输出这一组连通词
            if (component.size() > 1) {
                sb.append(String.join(", ", component)).append("\n");
            }
        }
        
        return sb.toString();
    }
}