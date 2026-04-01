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
 * 搜索词挖掘任务表
 * </p>
 */
@Data
@Accessors(chain = true)
@TableName("search_term_task")
public class SearchTermTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 搜索词 (去重核心)
     */
    private String term;

    /**
     * 搜索频次/热度
     */
    private Integer hitCount;

    /**
     * 状态: 0-待处理, 1-已收录, 2-已忽略
     */
    private Integer status;

    /**
     * 首次搜索时间
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
}
