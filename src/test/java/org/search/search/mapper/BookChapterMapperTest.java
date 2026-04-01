package org.search.search.mapper;

import org.junit.jupiter.api.Test;
import org.search.search.entity.Chapter;
import org.search.search.entity.TbBooks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 测试ChapterMapper和TbBookMapper接口
 */
@SpringBootTest
public class BookChapterMapperTest {

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private TbBookMapper tbBookMapper;

    /**
     * 测试查询所有章节
     */
    @Test
    public void testSelectAllChapters() {
        List<Chapter> chapters = chapterMapper.selectList(null);
        assertNotNull(chapters, "章节列表不应该为空");
        System.out.println("查询到的章节数量: " + chapters.size());
    }

    /**
     * 测试查询所有书籍
     */
    @Test
    public void testSelectAllBooks() {
        List<TbBooks> books = tbBookMapper.selectList(null);
        assertNotNull(books, "书籍列表不应该为空");
        System.out.println("查询到的书籍数量: " + books.size());
    }
}