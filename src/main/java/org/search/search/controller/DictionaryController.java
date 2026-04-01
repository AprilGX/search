package org.search.search.controller;

import lombok.RequiredArgsConstructor;
import org.search.search.service.DictionaryMiningService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryMiningService dictionaryMiningService;

    /**
     * IK 分词器热词接口
     * 返回格式：纯文本，每行一个词
     * 支持 304 缓存
     */
    @GetMapping(value = "/hot-words", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> getHotWords(WebRequest request) {
        // 1. 获取热词最后更新时间
        long lastModified = dictionaryMiningService.getLastHotWordModifiedTime();

        // 2. 检查 304 (If-Modified-Since)
        if (request.checkNotModified(lastModified)) {
            return null; // 返回 null 时，Spring 会自动发送 304 Not Modified 响应
        }

        // 3. 有更新，生成内容
        String content = dictionaryMiningService.getHotWordsDictionary();
        return ResponseEntity.ok()
                .header("Last-Modified", String.valueOf(lastModified))
                .header("ETag", String.valueOf(content.hashCode()))
                .body(content);
    }

    /**
     * Elasticsearch 同义词接口
     * 返回格式：纯文本，每行一条规则 (word => word, synonym)
     * 支持 304 缓存
     */
    @GetMapping(value = "/synonyms", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> getSynonyms(WebRequest request) {
        // 1. 获取同义词最后更新时间
        long lastModified = dictionaryMiningService.getLastSynonymModifiedTime();

        if (request.checkNotModified(lastModified)) {
            return null;
        }

        String content = dictionaryMiningService.getSynonymsDictionary();
        return ResponseEntity.ok()
                .header("Last-Modified", String.valueOf(lastModified))
                .header("ETag", String.valueOf(content.hashCode()))
                .body(content);
    }
}
