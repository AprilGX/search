package org.search.search.controller;

import lombok.RequiredArgsConstructor;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.search.search.index.ParagraphIndex;
import org.search.search.repository.ParagraphIndexRepository;
import org.search.search.service.DictionaryMiningService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ParagraphIndexRepository repository;
    private final DictionaryMiningService dictionaryMiningService;
    private final org.search.search.mapper.TbEventAssociationMapper eventAssociationMapper;
    private final org.search.search.mapper.TbEventMapper eventMapper;

    /**
     * GET请求示例:
     * /api/search/paragraph?keyword=历史&page=0&size=10
     */
    @GetMapping("/paragraph")
    public Map<String, Object> searchParagraph(
            @RequestParam String keyword,
            @RequestParam(required = false) String bookName,
            @RequestParam(required = false) String chapterName,
            @RequestParam(required = false) String author,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) throws IOException {

        // 异步记录搜索词，用于AI词典挖掘
        dictionaryMiningService.recordSearchTerm(keyword);

        // 调用仓库查询
        var result = repository.searchParagraphs(keyword, bookName, chapterName, author, page, size);

        Map<String, Object> res = new HashMap<>();

        // 正文命中（高亮或非高亮都可在 repository 内处理）
        res.put("paragraphItems", result.getContentHits());

        // 标题命中但正文未命中
        res.put("eventItems", result.getTitleHits());

        // 分页信息
//        long totalHits = result.getTotalHits();
        long paragraphTotal = result.getParagraphTotal();
        long eventTotal = result.getEventTotal();
        long totalHits = Math.max(paragraphTotal, eventTotal);
        res.put("page", page);
        res.put("size", size);
        res.put("paragraphTotal",paragraphTotal);
        res.put("eventTotal",eventTotal);

        long totalPages = (totalHits + size - 1) / size;
        res.put("totalPages", totalPages);

        return res;
    }

    /**
     * 批量获取段落对应的真实事件名称
     * @param paragraphIds 段落ID列表
     * @return Map<ParagraphId, List<EventTitle>>
     */
    @PostMapping("/events-by-paragraphs")
    public Map<Integer, List<String>> getEventsByParagraphIds(@RequestBody List<Integer> paragraphIds) {
        if (paragraphIds == null || paragraphIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 查询关联表
        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<org.search.search.entity.TbEventAssociation>();
        queryWrapper.in("association_id", paragraphIds);
        List<org.search.search.entity.TbEventAssociation> associations = eventAssociationMapper.selectList(queryWrapper);

        if (associations.isEmpty()) {
            return Collections.emptyMap();
        }

        // 2. 提取事件ID
        Set<Integer> eventIds = associations.stream()
                .map(org.search.search.entity.TbEventAssociation::getEventId)
                .collect(java.util.stream.Collectors.toSet());

        // 3. 查询事件表
        List<org.search.search.entity.TbEvent> events = eventMapper.selectBatchIds(eventIds);
        Map<Integer, String> eventIdToTitle = events.stream()
                .collect(java.util.stream.Collectors.toMap(org.search.search.entity.TbEvent::getEventId, org.search.search.entity.TbEvent::getEventTitle));

        // 4. 构建结果
        Map<Integer, List<String>> result = new HashMap<>();
        for (org.search.search.entity.TbEventAssociation assoc : associations) {
            String title = eventIdToTitle.get(assoc.getEventId());
            if (title != null) {
                result.computeIfAbsent(assoc.getAssociationId(), k -> new ArrayList<>()).add(title);
            }
        }
        return result;
    }
}

