package org.search.search.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.search.search.entity.VisitLog;
import org.search.search.mapper.VisitLogMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/monitor/stats")
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final VisitLogMapper visitLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final org.search.search.component.IpResolver ipResolver;

    private static final String REDIS_KEY_PREFIX = "monitor:stats:";

    @PostMapping("/track")
    public void track(@RequestBody Map<String, String> data, HttpServletRequest request) {
        String pagePath = data.get("path");
        String userAgent = request.getHeader("User-Agent");
        String ip = getIpAddress(request);

        CompletableFuture.runAsync(() -> {
            try {
                VisitLog log = new VisitLog();
                log.setIp(ip);
                log.setPagePath(pagePath);
                log.setUserAgent(userAgent);
                log.setCreateTime(LocalDateTime.now());
                
                // 1. 解析地理位置
                resolveLocation(log);

                // 2. 写入 MySQL 流水表 (异步，仅作归档)
                visitLogMapper.insert(log);

                // 3. Redis 实时计数 (Hash结构: monitor:stats:20251224:province -> { "北京": 10, "上海": 5 })
                String todayStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // 20251224
                String redisKey = REDIS_KEY_PREFIX + todayStr + ":province";
                
                String province = log.getProvince();
                if (province == null || province.isEmpty()) {
                    province = "未知";
                }
                
                // 对应省份计数 +1
                redisTemplate.opsForHash().increment(redisKey, province, 1);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 获取今日地图数据 (供前端大屏展示)
     */
    @GetMapping("/map-data")
    public Map<Object, Object> getMapData() {
        String todayStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String redisKey = REDIS_KEY_PREFIX + todayStr + ":province";
        return redisTemplate.opsForHash().entries(redisKey);
    }

    /**
     * 获取最新访问日志 (前20条)
     */
    @GetMapping("/latest-logs")
    public java.util.List<VisitLog> getLatestLogs() {
        return visitLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VisitLog>()
                        .orderByDesc(VisitLog::getCreateTime)
                        .last("LIMIT 20")
        );
    }

    /**
     * 获取真实IP
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                try {
                    InetAddress inet = InetAddress.getLocalHost();
                    ip = inet.getHostAddress();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (ip != null && ip.length() > 15) {
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }
        return ip;
    }

    /**
     * 解析地理位置
     */
    private void resolveLocation(VisitLog log) {
        String ip = log.getIp();
        if (ip == null) return;
        
        if (ip.startsWith("127.") || ip.startsWith("192.168.") || ip.startsWith("10.") || "0:0:0:0:0:0:0:1".equals(ip)) {
            log.setProvince("本地");
            log.setCity("本地");
            return;
        }

        // 使用离线库解析
        String province = ipResolver.resolveProvince(ip);
        if (province != null) {
            log.setProvince(province);
        } else {
            // 如果解析失败，先标记为未知
            log.setProvince("未知");
        }
    }
}