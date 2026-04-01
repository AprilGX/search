package org.search.search.index;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @author: GX
 * @time: 2025/11/11 8:33
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParagraphIndex {


    private Integer id; // 段落ID，对应数据库主键
    //书籍id

    private Integer bookId;
    //作者

    private String author;
    //书名
    private String title;
    //段落内容

    private String paragraphContent; // 段落内容，全文搜索主要字段
    //段落顺序

    private Integer paragraphOrder;
    //章节id

    private Integer chapterId;

    private String chapterContent;

    private String eventTitle;
}