package org.search.search.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Web请求日志切面
 * 打印接口调用的详细信息，方便排查 ES 是否来拉取词典
 */
@Aspect
@Component
@Slf4j
public class WebLogAspect {

    // 拦截 controller 包下的所有方法
    @Pointcut("execution(public * org.search.search.controller..*.*(..))")
    public void webLog() {}

    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取当前请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        boolean shouldLog = true;
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String requestURI = request.getRequestURI();
            
            // 排除不需要打印日志的路径
            if (requestURI.contains("/api/dict") || requestURI.contains("/api/monitor/stats")) {
                shouldLog = false;
            }

            if (shouldLog) {
                String url = request.getRequestURL().toString();
                String method = request.getMethod();
                String ip = request.getRemoteAddr();
                String userAgent = request.getHeader("User-Agent");
                
                // 简单打印日志
                log.info(">>> 收到请求: [{}] {}, IP: {}, User-Agent: {}", method, url, ip, userAgent);
            }
        }

        Object result = joinPoint.proceed();

        if (shouldLog) {
            long endTime = System.currentTimeMillis();
            log.info("<<< 请求结束: 耗时 {} 毫秒", (endTime - startTime));
        }
        
        return result;
    }
}
