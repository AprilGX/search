package org.search.search.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.*;


import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_event_ai")
public class TbEventAi {
    @TableId(value = "event_id", type = IdType.AUTO)
    private Integer eventId;

    @TableField("event_title")
    private String eventTitle;
}