package org.search.search.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.search.search.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.base-url}")
    private String baseUrl;

    @Value("${ai.deepseek.model}")
    private String model;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用 DeepSeek 分析搜索词
     *
     * @param terms 待分析的搜索词列表
     * @return AI 分析结果的 JSON 字符串（Raw String）
     */
    public String analyzeTerms(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return "[]";
        }

        try {
            // 1. 构造 Prompt
            String prompt = buildPrompt(terms);

            // 2. 构造 Request Body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "你是一个历史领域的专业助手，擅长中文分词和知识图谱构建。"),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.1); // 低温度，结果更确定
            requestBody.put("stream", false);
            // 强制 JSON 格式返回 (DeepSeek 支持 json_object 模式，但为了通用性，我们在 Prompt 里强调)
            requestBody.put("response_format", Map.of("type", "json_object"));

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 3. 发送请求
            String apiUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "chat/completions";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.info("Sending request to DeepSeek: Analyzing {} terms...", terms.size());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 4. 解析响应，提取 content
                JsonNode rootNode = objectMapper.readTree(response.body());
                String content = rootNode.path("choices").get(0).path("message").path("content").asText();
                log.info("DeepSeek analysis success.");
                return content;
            } else {
                log.error("DeepSeek API failed: Code={}, Body={}", response.statusCode(), response.body());
                return null;
            }

        } catch (Exception e) {
            log.error("Error calling DeepSeek API", e);
            return null;
        }
    }

    private String buildPrompt(List<String> terms) {
        return "请分析以下用户搜索词列表：\n" +
                objectMapper.valueToTree(terms).toString() + "\n\n" +
                "任务目标：\n" +
                "1. 识别其中属于【历史领域专有名词】的词汇。\n" +
                "2. 对于识别出的专有名词，判断是否需要同义词扩展（如“天下大赦”->“天下大赦,大赦”）。\n" +
                "3. 为每个词标注类型 (type)，必须从以下列表中选择：\n" +
                "   - history_event (历史事件，如：玄武门之变)\n" +
                "   - person (历史人物，如：李世民)\n" +
                "   - location (地名，如：长安)\n" +
                "   - official_title (官职/制度，如：尚书省)\n" +
                "   - book (典籍，如：史记)\n" +
                "   - other (其他专有名词)\n" +
                "4. 忽略无意义的虚词、通用词（如“那个”、“历史”）。\n\n" +
                "请严格以 JSON 格式返回结果，格式如下：\n" +
                "{\n" +
                "  \"results\": [\n" +
                "    {\n" +
                "      \"word\": \"天下大赦\",\n" +
                "      \"is_valid\": true,\n" +
                "      \"synonym_rule\": \"天下大赦, 大赦\",\n" +
                "      \"type\": \"history_event\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"word\": \"那个\",\n" +
                "      \"is_valid\": false\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}
