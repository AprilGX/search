package org.search.search.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 章节实体类
 * 对应数据库表：read_history_wisely.chapter
 */
@Data
@TableName("chapter")
public class Chapter {
    /**
     * 章节ID，唯一标识
     */
    private Integer id;
    
    /**
     * 周史名称，记录该章节所属周史内容
     */
    private String chapterContent;
    
    /**
     * 关联的书名ID，指向book表中的记录
     */
    private Integer bookId;
    
    /**
     * 父章节id
     */
    private Integer parentId;
}