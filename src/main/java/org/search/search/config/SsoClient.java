package org.search.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * SSO客户端工具类，用于与SSO认证中心通信
 */
@Component
public class SsoClient {
    private final String ssoServerUrl;
    private final String appDomain;
    private final RestTemplate restTemplate;
    
    public SsoClient(@Value("${sso.server.url:http://localhost:10000}") String ssoServerUrl,
                     @Value("${sso.app.domain:http://localhost:10050}") String appDomain) {
        this.ssoServerUrl = ssoServerUrl;
        this.appDomain = appDomain;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 验证JWT令牌
     */
    public boolean validateToken(String token) {
        System.out.println("[SsoClient] 开始验证token，长度: " + (token != null ? token.length() : "null"));
        
        if (token == null) {
            System.out.println("[SsoClient] Token为空，验证失败");
            return false;
        }
        
        try {
            String url = ssoServerUrl + "/sso/validate?token=" + token;
            System.out.println("[SsoClient] 发送验证请求到: " + url);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                boolean isValid = Boolean.TRUE.equals(body.getOrDefault("data", false));
                System.out.println("[SsoClient] Token验证响应状态码: " + response.getStatusCode() + ", 验证结果: " + isValid);
                return isValid;
            }
            
            System.out.println("[SsoClient] Token验证失败，状态码: " + response.getStatusCode());
            return false;
        } catch (Exception e) {
            // 记录异常
            System.err.println("[SsoClient] 验证token失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取用户信息
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(String token) {
        System.out.println("[SsoClient] 获取用户信息，token长度: " + (token != null ? token.length() : "null"));
        
        if (token == null) {
            System.out.println("[SsoClient] Token为空，返回空用户信息");
            return new HashMap<>();
        }
        
        try {
            String url = ssoServerUrl + "/sso/user-info?token=" + token;
            System.out.println("[SsoClient] 发送获取用户信息请求到: " + url);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            System.out.println("[SsoClient] 获取用户信息响应状态码: " + response.getStatusCode());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> userInfo = (Map<String, Object>) body.getOrDefault("data", new HashMap<>());
                System.out.println("[SsoClient] 成功获取用户信息，包含字段数: " + userInfo.size());
                return userInfo;
            }
            
            System.out.println("[SsoClient] 获取用户信息失败，响应状态码不为2xx或响应体为空");
            return new HashMap<>();
        } catch (Exception e) {
            // 记录异常
            System.err.println("[SsoClient] 获取用户信息失败: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }
    
    /**
     * 生成重定向到SSO登录页面的URL
     */
    public String generateLoginUrl(String finalRedirectUri) {
        System.out.println("[SsoClient] 开始生成SSO登录URL，最终重定向目标: " + finalRedirectUri);
        
        try {
            // 构建回调URL，SSO认证中心登录成功后应先回调到此应用的/auth/login-with-redirect端点
            String callbackUrl = appDomain + "/auth/login-with-redirect?redirect_uri=" + encodeUrl(finalRedirectUri);
            System.out.println("[SsoClient] 构建回调URL: " + callbackUrl);
            // 将回调URL作为SSO登录的redirect_uri参数
            String loginUrl = ssoServerUrl + "/login.html?redirect_uri=" + encodeUrl(callbackUrl);
            System.out.println("[SsoClient] 生成完整登录URL: " + loginUrl);
            return loginUrl;
        } catch (Exception e) {
            System.err.println("[SsoClient] 生成登录URL失败: " + e.getMessage());
            e.printStackTrace();
            // 出错时返回一个默认的登录URL
            String defaultLoginUrl = ssoServerUrl + "/login.html?redirect_uri=" + encodeUrl(appDomain + "/auth/login-with-redirect");
            System.out.println("[SsoClient] 返回默认登录URL: " + defaultLoginUrl);
            return defaultLoginUrl;
        }
    }
    
    private String encodeUrl(String url) {
        try {
            return java.net.URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return url;
        }
    }
//    public String extractToken(HttpServletRequest request) {
//        // Header
//        String bearerToken = request.getHeader("Authorization");
//        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
//            return bearerToken.substring(7);
//        }
//
//        // Cookie
//        if (request.getCookies() != null) {
//            for (javax.servlet.http.Cookie cookie : request.getCookies()) {
//                if ("auth_token".equals(cookie.getName())) {
//                    return cookie.getValue();
//                }
//            }
//        }
//
//        // URL参数
//        String token = request.getParameter("token");
//        if (token != null && !token.isEmpty()) return token;
//
//        return null;
//    }

}