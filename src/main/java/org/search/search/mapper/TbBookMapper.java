package org.search.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.search.search.entity.TbBooks;

/**
 * TbBook的Mapper接口，用于操作read_history_wisely.book表
 */
@Mapper
public interface TbBookMapper extends BaseMapper<TbBooks> {
    // BaseMapper已经提供了常用的CRUD方法，无需额外定义
}