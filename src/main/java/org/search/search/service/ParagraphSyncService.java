package org.search.search.service;

/**
 * 段落同步服务接口
 */
public interface ParagraphSyncService {
    
    /**
     * 同步所有段落数据到 ES
     */
    void syncAllParagraphsToEs();
}
