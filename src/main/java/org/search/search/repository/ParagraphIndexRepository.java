package org.search.search.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.search.search.index.ParagraphIndex;
import org.springframework.stereotype.Repository;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Repository
@RequiredArgsConstructor
public class ParagraphIndexRepository {

    private final RestHighLevelClient client;
    private static final String INDEX_NAME = "paragraph_index";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 删除并重新创建索引
     */
    public void recreateIndex() throws IOException {
        // 1. 检查索引是否存在
        GetIndexRequest getIndexRequest = new GetIndexRequest(INDEX_NAME);
        boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);

        // 2. 如果索引存在，先删除
        if (exists) {
            DeleteIndexRequest deleteRequest = new DeleteIndexRequest(INDEX_NAME);
            client.indices().delete(deleteRequest, RequestOptions.DEFAULT);
            System.out.println("索引已删除: " + INDEX_NAME);
        }

        // 3. 创建索引
        CreateIndexRequest request = new CreateIndexRequest(INDEX_NAME);

        // 配置
        request.settings(Settings.builder()
                .put("number_of_shards", 1)
                .put("number_of_replicas", 0)
                .put("refresh_interval", "1s")
                .put("index.max_result_window", 20000)
                // ============================================================
                // 毕设核心创新点：基于 AI 增强的混合分析器配置
                // 1. 定义同义词过滤器 (my_synonym_filter)：通过本地文件动态加载 AI 挖掘的同义词规则
                //    设置 update_interval 实现热更新 (每60秒检查一次)
                .put("analysis.filter.my_synonym_filter.type", "synonym_graph")
                .put("analysis.filter.my_synonym_filter.synonyms_path", "analysis-ik/synonyms.txt")
                .put("analysis.filter.my_synonym_filter.update_interval", "60s")
                // 2. 定义自定义分析器 (my_ai_analyzer)：组合 ik_smart 分词与同义词扩展
                .put("analysis.analyzer.my_ai_analyzer.tokenizer", "ik_smart")
                .putList("analysis.analyzer.my_ai_analyzer.filter", "my_synonym_filter")
                // ============================================================
        );

        // 新建的索引结构
        Map<String, Object> properties = new HashMap<>();

        properties.put("id", Map.of("type", "long"));
        // 章节内容（可用于筛选或显示）
        properties.put("paragraphContent", Map.of(
                "type", "text",
                "analyzer", "ik_smart",           // 索引时：只做最细粒度的分词，保持索引纯净
                "search_analyzer", "my_ai_analyzer", // 搜索时：启用 AI 同义词扩展
                "fields", Map.of(
                        "keyword", Map.of(
                                "type", "keyword",
                                "ignore_above", 256
                        )
                )
        ));

        properties.put("eventTitle", Map.of(
                "type", "text",
                "analyzer", "ik_smart",           // 索引时：只做最细粒度的分词
                "search_analyzer", "my_ai_analyzer", // 搜索时：启用 AI 同义词扩展
                "fields", Map.of(
                        "keyword", Map.of(
                                "type", "keyword",
                                "ignore_above", 256
                        )
                )
        ));

        properties.put("eventId", Map.of("type", "long"));
        properties.put("bookId", Map.of("type", "integer"));
        // 作者名称
        properties.put("author", Map.of(
                "type", "text",
                "analyzer", "ik_max_word",
                "fields", Map.of(
                        "keyword", Map.of(
                                "type", "keyword",
                                "ignore_above", 256
                        )
                )
        ));
        // 书名
        properties.put("title", Map.of(
                "type", "text",
                "analyzer", "ik_max_word",
                "fields", Map.of(
                        "keyword", Map.of(
                                "type", "keyword",
                                "ignore_above", 256
                        )
                )
        ));
        properties.put("chapterContent", Map.of(
                "type", "text",
                "analyzer", "ik_max_word",
                "fields", Map.of(
                        "keyword", Map.of(
                                "type", "keyword",
                                "ignore_above", 256
                        )
                )
        ));


        //目前用的是ES6.8.23
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", properties);

        request.mapping(mapping);

        // 执行创建
        client.indices().create(request, RequestOptions.DEFAULT);

        System.out.println("索引已重新创建: " + INDEX_NAME);
        System.out.println("索引设置：使用 ik_smart 分词器");
    }

    public void saveAll(List<ParagraphIndex> list) throws IOException {
        if (list == null || list.isEmpty()) return;

        BulkRequest bulkRequest = new BulkRequest();
        for (ParagraphIndex p : list) {
            bulkRequest.add(
                    new IndexRequest(INDEX_NAME, "_doc", String.valueOf(p.getId()))
                            .source(objectMapper.convertValue(p, Map.class))
            );
        }

        client.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    public MultiSearchResult searchParagraphs(
            String keyword,
            String bookName,
            String chapterName,
            String author,
            Integer page,
            Integer size
    ) throws IOException {

        int from = page * size;

        // =====================
        // 构建 filter 筛选条件
        // =====================
        BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
        if (bookName != null && !bookName.isEmpty()) {
            filterQuery.must(QueryBuilders.termQuery("title.keyword", bookName));
        }
        if (chapterName != null && !chapterName.isEmpty()) {
            filterQuery.must(QueryBuilders.termQuery("chapterContent.keyword", chapterName));
        }
        if (author != null && !author.isEmpty()) {
            filterQuery.must(QueryBuilders.termQuery("author.keyword", author));
        }

        // =====================
        // 1. 正文命中正文段落查询（带高亮）
        // =====================
        BoolQueryBuilder contentQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("paragraphContent", keyword))
                .filter(filterQuery);

        SearchSourceBuilder contentSource = new SearchSourceBuilder()
                .query(contentQuery)
                .from(from)
                .size(size)
                .highlighter(new HighlightBuilder()
                        .field("paragraphContent")
                        .preTags("<span class='highlight'>")
                        .postTags("</span>")
                        .fragmentSize(1000) // 避免内容被截断
                        .numOfFragments(0)//返回完整匹配结果
                );

        SearchRequest req1 = new SearchRequest(INDEX_NAME);
        req1.source(contentSource);

        SearchResponse resp1 = client.search(req1, RequestOptions.DEFAULT);
        List<ParagraphIndex> contentHits = convertHitsWithHighlight(resp1, "paragraphContent");
//        long totalHits = resp1.getHits().getTotalHits();
        long paragraphTotal = resp1.getHits().getTotalHits(); // paragraphItems 总数

        // =====================
        // 2. 标题命中但正文未命中（不高亮）
        // =====================
        BoolQueryBuilder titleOnlyQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("eventTitle", keyword))
                .mustNot(QueryBuilders.matchQuery("paragraphContent", keyword))
                .filter(filterQuery);

        SearchRequest req2 = new SearchRequest(INDEX_NAME);
        req2.source(new SearchSourceBuilder()
                .query(titleOnlyQuery)
                .from(from)
                .size(size));

        SearchResponse resp2 = client.search(req2, RequestOptions.DEFAULT);
        long eventTotal = resp2.getHits().getTotalHits(); // eventItems 总数
        long totalHits =eventTotal + paragraphTotal;
        List<ParagraphIndex> titleHits = convertHits(resp2);
        MultiSearchResult result = new MultiSearchResult(contentHits, titleHits);

        result.setEventTotal(eventTotal);
        result.setParagraphTotal(paragraphTotal);
        result.setTotalHits(totalHits);
        // 返回结果
        return result;
    }

    /**
     * 查询所有索引数据（分页）
     * 用于运维后台查看
     */
    public Map<String, Object> findAll(Integer page, Integer size) throws IOException {
        SearchRequest request = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.from(page * size);
        sourceBuilder.size(size);
        // 按 ID 排序，保证顺序稳定
        sourceBuilder.sort("id", org.elasticsearch.search.sort.SortOrder.ASC);
        
        request.source(sourceBuilder);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", response.getHits().getTotalHits());
        result.put("list", convertHits(response));
        
        return result;
    }

    /**
     * 更新单个文档的 paragraphContent 字段
     */
    public void updateContent(Integer id, String newContent) throws IOException {
        updateFields(id, Map.of("paragraphContent", newContent));
    }

    /**
     * 通用更新方法，更新指定字段
     */
    public void updateFields(Integer id, Map<String, Object> fields) throws IOException {
        org.elasticsearch.action.update.UpdateRequest request = new org.elasticsearch.action.update.UpdateRequest(INDEX_NAME, "_doc", String.valueOf(id));
        request.doc(fields);
        client.update(request, RequestOptions.DEFAULT);
    }

    private List<ParagraphIndex> convertHits(SearchResponse response) {
        List<ParagraphIndex> list = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            ParagraphIndex p = objectMapper.convertValue(hit.getSourceAsMap(), ParagraphIndex.class);
            list.add(p);
        }
        return list;
    }

    /** 转换查询结果并处理高亮 */
    private List<ParagraphIndex> convertHitsWithHighlight(SearchResponse response, String highlightField) {
        List<ParagraphIndex> list = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            ParagraphIndex p = objectMapper.convertValue(hit.getSourceAsMap(), ParagraphIndex.class);

            HighlightField hf = hit.getHighlightFields().get(highlightField);
            if (hf != null && hf.getFragments() != null) {
                StringBuilder sb = new StringBuilder();
                for (var fragment : hf.getFragments()) {
                    sb.append(fragment.string());
                }
                p.setParagraphContent(sb.toString());
            }

            list.add(p);
        }
        return list;
    }



    // =======================
    // 返回结果结构
    // =======================
    @Data
    public static class MultiSearchResult {
        private final List<ParagraphIndex> contentHits;
        private final List<ParagraphIndex> titleHits;
        private long eventTotal;
        private long paragraphTotal;
        private long totalHits;
    }
}

