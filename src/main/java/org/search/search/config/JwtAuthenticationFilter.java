package org.search.search.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * JWT令牌验证过滤器
 * 拦截请求并验证用户身份
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private SsoClient ssoClient;
    
    // 不需要认证的路径
    private final Set<String> EXCLUDED_PATHS = new HashSet<>(Arrays.asList(
//          "/api/event/**",        //关联事件相关
            "/api/event/list",      //获取所有事件
            "/",                    //首页
            "/login",               //需要认证的路由会由此去主站登录验证
            "/api/search/**",       //对内对外提供的方法
            "/api/monitor/**",      //【新增】监控接口白名单
            "/api/dict/**",         //【关键】放行词典接口，供ES和IK分词器调用
            "/sync/recreate-index",  //删除原本索引，重新建立索引（设置定时任务）
            "/help.html",
            "/search.html",
            "/样式/**",
            "/lib/**",
            "/favicon.ico",

            "/es/rebuild-paragraph-index"
    ));
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        // 添加调试日志 - 开始处理请求
        System.out.println("[JwtAuthenticationFilter] 开始处理请求: " + request.getRequestURI());
        
        // 检查是否从URL参数中获取token
        String token = extractToken(request);
        System.out.println("[JwtAuthenticationFilter] 提取到token: " + (token != null ? "已获取" : "null"));
        
        if (token != null && ssoClient.validateToken(token)) {
            // 验证成功，获取用户信息并设置到SecurityContext
            System.out.println("[JwtAuthenticationFilter] Token验证成功");
            Map<String, Object> userInfo = ssoClient.getUserInfo(token);
            String username = (String) userInfo.getOrDefault("username", "anonymous");
            
            System.out.println("[JwtAuthenticationFilter] 用户信息获取成功: " + username);
            
            // 创建认证对象
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            
            SecurityContextHolder.getContext().setAuthentication(authToken);
            System.out.println("[JwtAuthenticationFilter] 已设置认证信息到SecurityContext");
        } else {
            System.out.println("[JwtAuthenticationFilter] Token为空或验证失败");
            
            // 检查是否是SSO回调路径
            if (request.getRequestURI().contains("/auth/login-with-redirect")) {
                System.out.println("[JwtAuthenticationFilter] 是SSO回调路径，跳过重定向，继续处理");
                // 让LoginController处理SSO回调
            }
            // 检查是否需要认证
                else if (!isExcluded(request.getRequestURI())) {
                    System.out.println("[JwtAuthenticationFilter] 请求路径不是排除路径: " + request.getRequestURI());
                    if (isAjaxRequest(request)) {
                        System.out.println("[JwtAuthenticationFilter] 是Ajax请求，返回401状态码");
                        // 对于Ajax请求，返回401状态码，让前端处理重定向
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        
                        String redirectUri = buildRedirectUri(request);
                        String loginUrl = ssoClient.generateLoginUrl(redirectUri);
                        
                        response.getWriter().write("{\"success\":false,\"message\":\"请先登录\",\"loginUrl\":\"" + loginUrl + "\",\"code\":401}");
                        return;
                    } else {
                        System.out.println("[JwtAuthenticationFilter] 非Ajax请求，准备重定向到登录页面");
                        // 不是排除路径，非Ajax请求且未认证，重定向到SSO登录页面
                        String redirectUri = buildRedirectUri(request);
                        System.out.println("[JwtAuthenticationFilter] 构建重定向URI: " + redirectUri);
                        String loginUrl = ssoClient.generateLoginUrl(redirectUri);
                        System.out.println("[JwtAuthenticationFilter] 生成登录URL: " + loginUrl);
                        
                        // 确保重定向响应正确设置
                        response.setStatus(HttpServletResponse.SC_FOUND);
                        response.setHeader("Location", loginUrl);
                        System.out.println("[JwtAuthenticationFilter] 通过HTTP 302重定向到: " + loginUrl);
                        return;
                    }
                } else {
                System.out.println("[JwtAuthenticationFilter] 请求路径是排除路径，不需要认证: " + request.getRequestURI());
            }
        }
        
        // 对于排除路径或Ajax请求，继续过滤器链
        System.out.println("[JwtAuthenticationFilter] 继续过滤器链处理");
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求头、Cookie或URL参数提取token
     */
    private String extractToken(HttpServletRequest request) {
        // 从请求头提取token
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 从Cookie提取token
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (javax.servlet.http.Cookie cookie : cookies) {
                if ("auth_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        // 从URL参数提取token
        return request.getParameter("token");
    }
    
    /**
     * 判断是否为排除的路径
     */
    private boolean isExcluded(String uri) {
        // 静态资源文件类型检查
        if (uri.endsWith(".css") || uri.endsWith(".js") || 
            uri.endsWith(".png") || uri.endsWith(".jpg") || 
            uri.endsWith(".jpeg") || uri.endsWith(".gif") || 
            uri.endsWith(".ico") || uri.endsWith(".svg") || 
            uri.contains("font-awesome")) {
            return true;
        }
        
        // 精确路径匹配
        if (uri.equals("/favicon.ico")) {
            return true;
        }
        
        // 正确的路径前缀检查：检查URI是否以排除路径开头
        for (String excludedPath : EXCLUDED_PATHS) {
            if (excludedPath.endsWith("**")) {
                // 通配符匹配：检查URI是否以排除路径（去掉**）开头
                String basePath = excludedPath.substring(0, excludedPath.length() - 2);
                if (uri.startsWith(basePath)) {
                    return true;
                }
            } else if (uri.equals(excludedPath) || uri.startsWith(excludedPath + "/")) {
                // 精确匹配或子路径匹配
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否为Ajax请求
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        // 检查X-Requested-With头（传统Ajax请求）
        String xRequestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            return true;
        }
        
        // 检查Content-Type头（现代fetch API请求）
        String contentType = request.getHeader("Content-Type");
        if (contentType != null && contentType.contains("application/json")) {
            return true;
        }
        
        // 检查Accept头（期望JSON响应）
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 构建重定向URI
     * 对于Ajax请求，重定向到当前页面而不是API接口URL
     */
    private String buildRedirectUri(HttpServletRequest request) {
        // 优先使用前端传递的 redirect_uri 参数
        String redirectUriParam = request.getParameter("redirect_uri");
        if (redirectUriParam != null && !redirectUriParam.isEmpty()) {
            System.out.println("[JwtAuthenticationFilter] 使用前端传递的redirect_uri: " + redirectUriParam);
            return redirectUriParam;
        }

        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(request.getScheme()).append("://")
                .append(request.getServerName())
                .append(":")
                .append(request.getServerPort());

        // 对于Ajax请求，重定向到当前页面而不是API接口URL
        if (isAjaxRequest(request)) {
            // 获取Referer头，即用户发起请求的页面URL
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.isEmpty()) {
                System.out.println("[JwtAuthenticationFilter] 使用Referer作为重定向目标: " + referer);
                return referer;
            }

            // 如果没有Referer，则重定向到根路径
            uriBuilder.append("/");
            System.out.println("[JwtAuthenticationFilter] 未找到Referer，使用根路径作为重定向目标: " + uriBuilder.toString());
        } else {
            // 对于非Ajax请求，使用原始请求URI
            uriBuilder.append(request.getRequestURI());

            String queryString = request.getQueryString();
            if (queryString != null) {
                uriBuilder.append("?").append(queryString);
            }
            System.out.println("[JwtAuthenticationFilter] 非Ajax请求，使用原始URI作为重定向目标: " + uriBuilder.toString());
        }

        return uriBuilder.toString();
    }
}