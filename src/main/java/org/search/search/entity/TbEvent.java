package org.search.search.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_event")
public class TbEvent implements Serializable {

    @TableId(value = "event_id", type = IdType.AUTO)
    private Integer eventId;

    @TableField("event_title")
    private String eventTitle;
}