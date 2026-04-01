package org.search.search.service;

import org.springframework.scheduling.annotation.Scheduled;

public interface DictionaryMiningService {

    /**
     * 极速记录用户搜索词 (Redis 原子操作)
     * @param keyword 用户输入的原始搜索词
     */
    void recordSearchTerm(String keyword);

    /**
     * 定时任务：每分钟将 Redis 中的热词同步到 MySQL
     */
    void syncToDatabase();

    /**
     * 定时任务：每5分钟调用 AI 分析待处理的搜索词
     */
    void mineTermsFromAi();

    @Scheduled(fixedRate = 60000)
    void syncFilesToLocal();

    /**
     * 获取 IK 分词器所需的热词列表 (纯文本格式)
     */
    String getHotWordsDictionary();

    /**
     * 获取 ES 所需的同义词规则列表 (纯文本格式)
     */
    String getSynonymsDictionary();

    /**
     * 获取词典最后一次更新的时间戳 (兼容旧接口，取最大值)
     */
    long getLastModifiedTime();

    long getLastHotWordModifiedTime();

    long getLastSynonymModifiedTime();
}
