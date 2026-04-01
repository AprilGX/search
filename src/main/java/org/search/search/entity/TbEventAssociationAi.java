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
 * (TbEventAssociationAi)实体类
 *
 * @author makejava
 * @since 2025-11-10 18:26:53
 */
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_event_association_ai")
public class TbEventAssociationAi {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("association_id")
    private Integer associationId;

    @TableField("event_id")
    private Integer eventId;
}
