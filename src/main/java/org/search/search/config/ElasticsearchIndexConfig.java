package org.search.search.config;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Elasticsearch索引初始化配置
 */
//spring自动扫描并且实例化该类
    // 实现CommandLineRunner接口，在应用启动时执行run方法
@Component
public class ElasticsearchIndexConfig implements CommandLineRunner {

    private static final String INDEX_NAME = "paragraph_index";
    
    @Autowired
    private RestHighLevelClient client;

    @Override
    public void run(String... args) throws Exception {
        try {
            // 检查并创建索引
            createIndexIfNotExists();
        } catch (Exception e) {
            System.err.println("索引初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createIndexIfNotExists() throws IOException {
        // 检查索引是否存在
        GetIndexRequest getIndexRequest = new GetIndexRequest(INDEX_NAME);
        boolean exists = false;
        try {
            exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            // 如果出现异常，假设索引不存在
            System.out.println("检查索引时出现异常: " + e.getMessage());
        }
        
        if (!exists) {
            System.out.println("正在创建索引: " + INDEX_NAME);
            
            // 创建索引请求
            CreateIndexRequest request = new CreateIndexRequest(INDEX_NAME);
            
            // 设置索引配置
            request.settings(Settings.builder()
                    .put("number_of_shards", 1)
                    .put("number_of_replicas", 0)
                    .put("refresh_interval", "1s")
                    .put("index.max_result_window", 20000)
            );

            // 设置 Mapping
            // 核心字段使用 IK 分词，索引时细粒度(ik_max_word)，搜索时粗粒度(ik_smart)
            // 另外保留 keyword 类型用于精确过滤
            String mapping = "{\n" +
                    "  \"properties\": {\n" +
                    "    \"paragraphContent\": {\n" +
                    "      \"type\": \"text\",\n" +
                    "      \"analyzer\": \"ik_max_word\",\n" +
                    "      \"search_analyzer\": \"ik_smart\"\n" +
                    "    },\n" +
                    "    \"eventTitle\": {\n" +
                    "      \"type\": \"text\",\n" +
                    "      \"analyzer\": \"ik_max_word\",\n" +
                    "      \"search_analyzer\": \"ik_smart\"\n" +
                    "    },\n" +
                    "    \"title\": {\n" +
                    "      \"type\": \"keyword\"\n" +
                    "    },\n" +
                    "    \"chapterContent\": {\n" +
                    "      \"type\": \"keyword\"\n" +
                    "    },\n" +
                    "    \"author\": {\n" +
                    "      \"type\": \"keyword\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";
            request.mapping(mapping, org.elasticsearch.common.xcontent.XContentType.JSON);
            
            // 执行创建索引请求
            client.indices().create(request, RequestOptions.DEFAULT);
            System.out.println("索引创建成功: " + INDEX_NAME);
        } else {
            System.out.println("索引已存在: " + INDEX_NAME);
        }
    }
}