package org.search.search.service;

import org.search.search.entity.TbEvent;
import org.search.search.entity.TbEventAssociation;
import java.util.List;
import java.util.Map;

/**
 * 事件关联服务接口
 */
public interface EventAssociationService {
    
    /**
     * 获取所有事件列表
     * @return 事件列表
     */
    List<TbEvent> getAllEvents();
    
    /**
     * 创建段落与事件的关联
     * @param paragraphId 段落ID
     * @param eventId 事件ID
     * @return 创建的关联对象
     */
    TbEventAssociation createAssociation(Integer paragraphId, Integer eventId);
    
    /**
     * 批量创建段落与事件的关联
     * @param associations 关联信息列表，每个元素包含段落ID和事件ID
     * @return 包含成功数量和重复段落信息的结果对象
     */
    Map<String, Object> batchCreateAssociations(List<AssociationRequest> associations);
    
    /**
     * 关联请求内部类
     */
    class AssociationRequest {
        private Integer paragraphId;
        private Integer eventId;
        
        // Getters and setters
        public Integer getParagraphId() {
            return paragraphId;
        }
        
        public void setParagraphId(Integer paragraphId) {
            this.paragraphId = paragraphId;
        }
        
        public Integer getEventId() {
            return eventId;
        }
        
        public void setEventId(Integer eventId) {
            this.eventId = eventId;
        }
    }
}