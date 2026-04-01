package org.search.search.controller;

import org.search.search.entity.TbEvent;
import org.search.search.entity.TbEventAssociation;
import org.search.search.service.EventAssociationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 事件关联控制器
 */
@RestController
@RequestMapping("/api/event")
public class EventAssociationController {
    
    @Autowired
    private EventAssociationService eventAssociationService;
    
    /**
     * 获取所有事件列表
     * 用于弹窗显示事件选择
     * 
     * @return 事件列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<TbEvent>> getAllEvents() {
        List<TbEvent> events = eventAssociationService.getAllEvents();
        return ResponseEntity.ok(events);
    }
    
    /**
     * 创建单个段落与事件的关联
     * 
     * @param paragraphId 段落ID
     * @param eventId 事件ID
     * @return 关联结果
     */
    @PostMapping("/associate")
    public ResponseEntity<Map<String, Object>> associateParagraphWithEvent(
            @RequestParam Integer paragraphId,
            @RequestParam Integer eventId) {
        
        try {
            TbEventAssociation association = eventAssociationService.createAssociation(paragraphId, eventId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "关联成功",
                "data", association
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "关联失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 批量创建段落与事件的关联
     * 前端传递多个段落ID和同一个事件ID进行关联
     * 
     * @param requestData 请求数据，包含段落ID列表和事件ID
     * @return 关联结果
     */
    @PostMapping("/associate/batch")
    public ResponseEntity<Map<String, Object>> batchAssociateParagraphsWithEvent(
            @RequestBody Map<String, Object> requestData) {
        
        try {
            // 从请求数据中获取段落ID列表和事件ID
            List<Integer> paragraphIds = (List<Integer>) requestData.get("paragraphIds");
            Integer eventId = (Integer) requestData.get("eventId");
            
            if (paragraphIds == null || paragraphIds.isEmpty() || eventId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "参数错误：段落ID列表和事件ID不能为空"
                ));
            }
            
            // 构建关联请求列表
            List<EventAssociationService.AssociationRequest> associations = paragraphIds.stream()
                    .map(paragraphId -> {
                        EventAssociationService.AssociationRequest request = new EventAssociationService.AssociationRequest();
                        request.setParagraphId(paragraphId);
                        request.setEventId(eventId);
                        return request;
                    })
                    .toList();
            
            // 批量创建关联，接收Map返回值
            Map<String, Object> result = eventAssociationService.batchCreateAssociations(associations);
            
            // 构建响应
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "批量关联成功",
                "successCount", result.get("successCount"),
                "duplicateParagraphIds", result.get("duplicateParagraphIds"),
                "data", Map.of(
                    "total", paragraphIds.size(),
                    "successCount", result.get("successCount")
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "批量关联失败: " + e.getMessage()
            ));
        }
    }
}