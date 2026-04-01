package org.search.search.service;

import java.util.List;

public interface DeepSeekService {
    /**
     * 调用 DeepSeek 分析搜索词
     *
     * @param terms 待分析的搜索词列表
     * @return AI 分析结果的 JSON 字符串（Raw String）
     */
    String analyzeTerms(List<String> terms);
}
