package org.search.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.search.search.entity.TbEventAssociation;

import java.util.Set;
import java.util.List;

/**
 * @description: TbEventAssociation实体的Mapper接口
 * @author: System
 * @time: 2024
 */
@Mapper
public interface TbEventAssociationMapper extends BaseMapper<TbEventAssociation> {
    
    /**
     * 查找指定事件ID和段落ID集合中的已存在关联
     * @param eventId 事件ID
     * @param paragraphIds 段落ID集合
     * @return 已存在关联的段落ID列表
     */
    List<Integer> findExistingAssociations(@Param("eventId") Integer eventId, @Param("paragraphIds") Set<Integer> paragraphIds);
    
    // checkAssociationExists方法已移除，因为在代码库中未被使用
}