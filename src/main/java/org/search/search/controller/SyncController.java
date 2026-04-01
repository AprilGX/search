package org.search.search.controller;

import org.search.search.component.SyncTaskMonitor;
import org.search.search.repository.ParagraphIndexRepository;
import org.search.search.service.ParagraphSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/search/sync")
public class SyncController {

    private final ParagraphSyncService paragraphSyncService;
    private final ParagraphIndexRepository paragraphIndexRepository;
    private final SyncTaskMonitor monitor;

    @Autowired
    public SyncController(ParagraphSyncService paragraphSyncService, 
                          ParagraphIndexRepository paragraphIndexRepository,
                          SyncTaskMonitor monitor) {
        this.paragraphSyncService = paragraphSyncService;
        this.paragraphIndexRepository = paragraphIndexRepository;
        this.monitor = monitor;
    }

    @GetMapping("/paragraphs")
    public String syncParagraphs() {
        // 简单同步暂不接入监控，或后续接入
        paragraphSyncService.syncAllParagraphsToEs();
        return "段落同步任务已启动，请查看日志进度";
    }

    @GetMapping("/status")
    public SyncTaskMonitor getStatus() {
        return monitor;
    }

    /**
     * 覆盖索引：异步执行
     */
    @PostMapping("/recreate-index")
    public String recreateIndexAndSync() {
        if (monitor.getStatus() == 1) {
            return "任务正在进行中，请勿重复提交";
        }

        // 异步启动
        new Thread(() -> {
            try {
                monitor.startTask();
                
                monitor.addLog("正在删除并重建索引结构...");
                // 1. 先删除并重新创建索引
                paragraphIndexRepository.recreateIndex();
                monitor.addLog("索引结构重建完成");

                // 2. 重新同步所有数据
                paragraphSyncService.syncAllParagraphsToEs();
                
                monitor.finishTask();
            } catch (Exception e) {
                e.printStackTrace();
                monitor.failTask(e.getMessage());
            }
        }).start();

        return "索引重建任务已后台启动";
    }
}