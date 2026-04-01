package org.search.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.search.search.entity.Chapter;

/**
 * 章节Mapper接口
 * 使用MyBatis Plus框架，自动继承基本的CRUD操作
 */
@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {
}