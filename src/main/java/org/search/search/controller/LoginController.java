package org.search.search.controller;

import org.search.search.config.SsoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录控制器，处理与登录相关的请求
 */
@Controller
public class LoginController {

    @Autowired
    private SsoClient ssoClient;

    /**
     * 处理登录请求，重定向到SSO认证中心
     */
    @GetMapping("/login")
    public void login(HttpServletRequest request, HttpServletResponse response, 
                      @RequestParam(required = false) String redirect_uri) throws IOException {
        System.out.println("[LoginController] 接收到/login请求");
        System.out.println("[LoginController] 请求参数redirect_uri: " + redirect_uri);
        
        // 如果有redirect_uri参数，使用它，否则使用当前页面URL
        String redirectUri = redirect_uri;
        if (redirectUri == null) {
            System.out.println("[LoginController] 未提供redirect_uri参数，使用默认值");
            StringBuilder uriBuilder = new StringBuilder();
            uriBuilder.append(request.getScheme()).append("://")
                    .append(request.getServerName())
                    .append(":")
                    .append(request.getServerPort())
                    .append("/"); // 根路径作为默认重定向目标
            redirectUri = uriBuilder.toString();
            System.out.println("[LoginController] 构建默认重定向URI: " + redirectUri);
        }

        // 生成重定向到SSO登录页面的URL
        System.out.println("[LoginController] 调用SsoClient.generateLoginUrl生成登录URL");
        String loginUrl = ssoClient.generateLoginUrl(redirectUri);
        // 输出重定向URL，用于调试
        System.out.println("[LoginController] 生成的登录URL: " + loginUrl);
        System.out.println("[LoginController] 执行重定向到: " + loginUrl);
        // 重定向到SSO服务器
        response.sendRedirect(loginUrl);
    }

    /**
     * 处理SSO服务器回调，验证token并存储
     */
    @GetMapping("/auth/login-with-redirect")
    public void ssoCallback(HttpServletRequest request, HttpServletResponse response, 
                           @RequestParam String token, 
                           @RequestParam(required = false) String redirect_uri) throws IOException {
        System.out.println("[LoginController] 接收到SSO回调请求: /auth/login-with-redirect");
        System.out.println("[LoginController] 回调参数token: " + (token != null ? "已获取" : "null"));
        System.out.println("[LoginController] 回调参数redirect_uri: " + redirect_uri);
        
        try {
            // 验证token
            System.out.println("[LoginController] 开始验证token");
            boolean tokenValid = ssoClient.validateToken(token);
            System.out.println("[LoginController] Token验证结果: " + (tokenValid ? "成功" : "失败"));
            
            if (tokenValid) {
                // token验证成功，构建重定向URL
                System.out.println("[LoginController] Token验证成功，准备重定向回原始页面");
                
                // 确保redirectUri有效，如果为空或无效则默认重定向到根路径
                String finalRedirectUri = redirect_uri;
                if (finalRedirectUri == null || finalRedirectUri.isEmpty()) {
                    finalRedirectUri = "/";
                    System.out.println("[LoginController] redirect_uri为空，使用默认值: " + finalRedirectUri);
                }
                
                System.out.println("[LoginController] 最终重定向目标: " + finalRedirectUri);
                
                // 使用Cookie存储token，避免直接在URL中暴露
                javax.servlet.http.Cookie tokenCookie = new javax.servlet.http.Cookie("auth_token", token);
                tokenCookie.setPath("/");
                tokenCookie.setHttpOnly(true); // 防止JavaScript访问
                // tokenCookie.setSecure(true); // 在生产环境中应启用HTTPS
                tokenCookie.setMaxAge(3600); // 设置Cookie过期时间（1小时）
                response.addCookie(tokenCookie);
                
                System.out.println("[LoginController] 已将token存储在Cookie中，执行重定向: " + finalRedirectUri);
                // 重定向回客户端应用，不再在URL中携带token参数
                response.sendRedirect(finalRedirectUri);
            } else {
                // token验证失败，重定向到登录页面
                System.out.println("[LoginController] Token验证失败，重定向到登录页面");
                String fallbackRedirectUri = redirect_uri != null ? redirect_uri : "/";
                String loginRedirectUrl = "/login?redirect_uri=" + 
                        java.net.URLEncoder.encode(fallbackRedirectUri, "UTF-8");
                System.out.println("[LoginController] 执行重定向到登录页面: " + loginRedirectUrl);
                response.sendRedirect(loginRedirectUrl);
            }
        } catch (Exception e) {
            // 捕获任何异常，记录错误并确保重定向到登录页面
            System.out.println("[LoginController] 处理SSO回调时发生异常: " + e.getMessage());
            e.printStackTrace();
            String fallbackRedirectUri = redirect_uri != null ? redirect_uri : "/";
            String loginRedirectUrl = "/login?redirect_uri=" + 
                    java.net.URLEncoder.encode(fallbackRedirectUri, "UTF-8");
            response.sendRedirect(loginRedirectUrl);
        }
    }
}