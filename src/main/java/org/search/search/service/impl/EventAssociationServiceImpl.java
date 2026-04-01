package org.search.search.service.impl;

import org.search.search.entity.TbEvent;
import org.search.search.entity.TbEventAssociation;
import org.search.search.mapper.TbEventMapper;
import org.search.search.mapper.TbEventAssociationMapper;
import org.search.search.service.EventAssociationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 事件关联服务实现类
 */
@Service
public class EventAssociationServiceImpl implements EventAssociationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventAssociationServiceImpl.class);
    
    @Autowired
    private TbEventMapper tbEventMapper;
    
    @Autowired
    private TbEventAssociationMapper tbEventAssociationMapper;
    
    /**
     * 获取所有事件列表
     * @return 事件列表
     */
    @Override
    public List<TbEvent> getAllEvents() {
        // 使用MyBatis-Plus的BaseMapper提供的selectList方法，参数为null表示查询所有
        return tbEventMapper.selectList(null);
    }
    
    /**
     * 创建段落与事件的关联
     * @param paragraphId 段落ID
     * @param eventId 事件ID
     * @return 创建的关联对象
     * @throws Exception 当关联已存在或其他错误发生时抛出
     */
    @Override
    public TbEventAssociation createAssociation(Integer paragraphId, Integer eventId) {
        // 直接创建关联对象，不再进行检查，因为在batchCreateAssociations方法中已经做了充分的检查
        // 单独调用此方法时，由调用方负责检查重复
        logger.debug("创建关联：段落ID={}, 事件ID={}", paragraphId, eventId);
        
        // 创建关联对象
        TbEventAssociation association = new TbEventAssociation();
        association.setEventId(eventId);
        association.setAssociationId(paragraphId); // 根据数据库结构，association_id存储的是段落ID
        
        // 插入数据库
        tbEventAssociationMapper.insert(association);
        
        return association;
    }
    
    /**
     * 批量创建段落与事件的关联
     * @param associations 关联信息列表
     * @return 包含成功数量和重复段落信息的结果对象
     */
    @Override
    @Transactional
    public Map<String, Object> batchCreateAssociations(List<AssociationRequest> associations) {

        if (associations == null || associations.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("successCount", 0);
            emptyResult.put("duplicateParagraphIds", new ArrayList<>());
            return emptyResult;
        }

        int successCount = 0;
        List<Integer> duplicateParagraphIds = new ArrayList<>();

        // 按事件ID分组
        Map<Integer, List<AssociationRequest>> groupedByEvent = associations.stream()
                .filter(r -> r != null && r.getParagraphId() != null && r.getEventId() != null)
                .collect(Collectors.groupingBy(AssociationRequest::getEventId));

        for (Map.Entry<Integer, List<AssociationRequest>> entry : groupedByEvent.entrySet()) {
            Integer eventId = entry.getKey();
            List<AssociationRequest> requests = entry.getValue();

            // 收集 paragraphId
            Set<Integer> paragraphIds = requests.stream()
                    .map(AssociationRequest::getParagraphId)
                    .collect(Collectors.toSet());

            // 批量查询已存在的关联（只查一次）
            List<Integer> existingParagraphIds =
                    tbEventAssociationMapper.findExistingAssociations(eventId, paragraphIds);

            Set<Integer> existingSet = new HashSet<>(existingParagraphIds);

            for (AssociationRequest req : requests) {
                Integer paragraphId = req.getParagraphId();

                if (existingSet.contains(paragraphId)) {
                    // 已存在——记录重复
                    duplicateParagraphIds.add(paragraphId);
                    continue;
                }

                try {
                    // 不存在——插入
                    createAssociation(paragraphId, eventId);
                    successCount++;
                } catch (Exception e) {
                    // 捕获 MySQL 唯一冲突（并发时可能发生）
                    duplicateParagraphIds.add(paragraphId);
                    logger.error("并发插入导致的重复: 段落 {} - 事件 {}", paragraphId, eventId, e);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("duplicateParagraphIds", duplicateParagraphIds);

        // 类型判断（返回前端用）
        if (duplicateParagraphIds.isEmpty()) {
            result.put("messageType", "allSuccess");
        } else if (successCount == 0) {
            result.put("messageType", "allDuplicate");
        } else {
            result.put("messageType", "partialSuccess");
        }

        return result;
    }

}