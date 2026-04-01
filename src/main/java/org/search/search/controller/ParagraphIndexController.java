package org.search.search.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.search.search.service.ParagraphSyncService;
import org.search.search.repository.ParagraphIndexRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ParagraphIndexController {

    private final ParagraphIndexRepository paragraphIndexRepository;
    private final ParagraphSyncService paragraphSyncService;

    /**
     * 删除旧索引，新建索引，并同步 MySQL 数据到 Elasticsearch
     */
    @GetMapping("/es/rebuild-paragraph-index")
    public String rebuildParagraphIndex() {
        try {
            // 1. 删除旧索引并重新创建
            paragraphIndexRepository.recreateIndex();
            log.info("✅ Elasticsearch 索引已重新创建");

            // 2. 同步 MySQL 数据到 Elasticsearch
            paragraphSyncService.syncAllParagraphsToEs();
            log.info("✅ MySQL 数据已同步到 Elasticsearch");

            return "索引重建完成，数据已同步！";
        } catch (Exception e) {
            log.error("索引重建或数据同步失败", e);
            return "索引重建或数据同步失败：" + e.getMessage();
        }
    }
}
