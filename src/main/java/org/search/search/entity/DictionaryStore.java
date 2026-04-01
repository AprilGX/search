package org.search.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * AI动态词典库
 * </p>
 */
@Data
@Accessors(chain = true)
@TableName("dictionary_store")
public class DictionaryStore implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 核心词汇 (存入 IK 词典)
     */
    private String word;

    /**
     * 同义词规则 (例如: 天下大赦 => 天下大赦, 大赦)
     */
    private String synonymRule;

    /**
     * 词汇类型 (history/person/place...)
     */
    private String type;

    /**
     * 是否启用: 1-启用, 0-禁用
     */
    private Boolean isEnabled;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
