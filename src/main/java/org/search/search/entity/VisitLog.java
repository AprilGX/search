package org.search.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_visit_log")
public class VisitLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String ip;
    private String province;
    private String city;
    private String pagePath;
    private String userAgent;
    private LocalDateTime createTime;
}