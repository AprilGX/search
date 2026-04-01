package org.search.search.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 搜索结果封装类
 */
@Data
public class SearchResult {
    @Setter
    private long total; // 总条数
    @Getter
    @Setter
    private List<Map<String, Object>> items; // 结果列表
    private long paragraphTotal;
    private long eventTotal;
    private List<Map<String, Object>> paragraphItems;
    private List<Map<String, Object>> eventItems;
    public long getTotal() {
        return total;
    }

}