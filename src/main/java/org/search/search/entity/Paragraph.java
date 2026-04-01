package org.search.search.entity;

import java.io.Serializable;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

/**
 * 存储段落信息(Paragraph)实体类
 *
 * @author makejava
 * @since 2025-11-10 18:12:58
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("paragraph")
public class Paragraph implements Serializable {

    private static final long serialVersionUID = -13908839465073756L;

        /**
     * 段落ID，唯一标识
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

        private String paragraphContent;
    /**
     * 段落顺序，确定段落显示顺序
     */
    private Integer paragraphOrder;
    /**
     * 所属章节ID，关联chapter表
     */
    private Integer chapterId;
    /**
     * 段落样式信息，JSON格式
     */
    private String styleParagraphContent;
    /**
     * 中间表自增id
     */
    private Integer eventAssociationId;
    /**
     * ai中间表自增id
     */
    private Integer eventAssociationAiId;

}

