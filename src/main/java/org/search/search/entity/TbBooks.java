package org.search.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * TbBook实体类，对应read_history_wisely.book表
 */
@Data
@TableName("tb_books")
public class TbBooks {

    /**
     * 书籍ID，自增主键
     */
    @TableId(type = IdType.AUTO)
    private Integer bookId;

    /**
     * 作者
     */
    private String author;

    /**
     * 分类
     */
    private String category;

    /**
     * 创建者ID
     */
    private Long createdBy;

    /**
     * 书籍描述
     */
    private String description;

    /**
     * 书籍标题
     */
    private String title;
}