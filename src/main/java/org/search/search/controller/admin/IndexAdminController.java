package org.search.search.controller.admin;

import lombok.RequiredArgsConstructor;
import org.search.search.entity.Paragraph;
import org.search.search.entity.TbEventAi;
import org.search.search.entity.TbEventAssociationAi;
import org.search.search.index.ParagraphIndex;
import org.search.search.mapper.ParagraphMapper;
import org.search.search.mapper.TbEventAiMapper;
import org.search.search.mapper.TbEventAssociationAiMapper;
import org.search.search.repository.ParagraphIndexRepository;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 索引管理接口
 */
@RestController
@RequestMapping("/api/search/admin/index")
@RequiredArgsConstructor
public class IndexAdminController {

    private final ParagraphIndexRepository repository;
    private final ParagraphMapper paragraphMapper;
    private final TbEventAiMapper tbEventAiMapper;
    private final TbEventAssociationAiMapper tbEventAssociationAiMapper;

    /**
     * 分页查看索引内容（全部）
     * 用于运维调试
     */
    @GetMapping("/list")
    public Map<String, Object> listIndexContent(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) throws IOException {
        
        // 调用 Repository 中的全量查询方法
        return repository.findAll(page, size);
    }

    /**
     * 更新索引文档内容（同步更新数据库）
     */
    @PostMapping("/update")
    public Map<String, Object> updateIndexContent(@RequestBody Map<String, Object> body) {
        try {
            Integer id = (Integer) body.get("id");
            String content = (String) body.get("content");
            String eventTitle = (String) body.get("eventTitle");
            
            if (id == null) {
                throw new IllegalArgumentException("ID is required");
            }

            Map<String, Object> esUpdateFields = new HashMap<>();

            // 1. 更新数据库段落内容
            if (content != null) {
                Paragraph p = new Paragraph();
                p.setId(id);
                p.setParagraphContent(content);
                paragraphMapper.updateById(p);
                esUpdateFields.put("paragraphContent", content);
            }

            // 2. 更新 AI 标签
            if (eventTitle != null) {
                Paragraph p = paragraphMapper.selectById(id);
                if (p != null) {
                    Integer assocAiId = p.getEventAssociationAiId();
                    if (assocAiId != null) {
                        // 1:1 关系逻辑：直接修改关联的标签记录
                        TbEventAssociationAi assoc = tbEventAssociationAiMapper.selectById(assocAiId);
                        if (assoc != null && assoc.getEventId() != null) {
                            TbEventAi eventAi = new TbEventAi();
                            eventAi.setEventId(assoc.getEventId());
                            eventAi.setEventTitle(eventTitle);
                            tbEventAiMapper.updateById(eventAi);
                        }
                    } else {
                        // 如果原本没有关联，则新建并绑定
                        TbEventAi eventAi = new TbEventAi();
                        eventAi.setEventTitle(eventTitle);
                        tbEventAiMapper.insert(eventAi);

                        TbEventAssociationAi assoc = new TbEventAssociationAi();
                        assoc.setAssociationId(id); // 关联当前段落ID
                        assoc.setEventId(eventAi.getEventId());
                        tbEventAssociationAiMapper.insert(assoc);
                        
                        // 回写段落表
                        p.setEventAssociationAiId(assoc.getId());
                        paragraphMapper.updateById(p);
                    }
                }
                esUpdateFields.put("eventTitle", eventTitle);
            }

            // 3. 同步更新 ES 索引
            if (!esUpdateFields.isEmpty()) {
                repository.updateFields(id, esUpdateFields);
            }
            
            return Map.of("code", 200, "message", "更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("code", 500, "message", "更新失败: " + e.getMessage());
        }
    }
}
