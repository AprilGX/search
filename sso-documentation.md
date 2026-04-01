# 单点登录（SSO）系统文档

## 1. 系统概述

本文档详细介绍了历史阅读系统（Historical Reading System）中集成的单点登录（Single Sign-On, SSO）功能，旨在为其他应用提供统一的认证服务，实现一次登录、多处访问的用户体验。

## 1.1 使用说明概述

本单点登录系统支持应用间跳转登录功能，允许第三方应用（如B应用）将未登录用户重定向到认证中心（历史阅读系统）进行登录，登录成功后再重定向回第三方应用，并携带JWT令牌进行身份验证。

### 1.1 功能特点

- **基于JWT的无状态认证**：使用JSON Web Token进行用户身份验证，实现无状态会话管理
- **跨域支持**：通过CORS配置支持跨域请求，允许不同域名的应用集成
- **安全可靠**：支持会话状态验证，防止CSRF攻击
- **易于集成**：提供简洁明了的API接口，方便其他应用快速集成
- **向后兼容**：保留原有认证机制，确保现有功能不受影响

### 1.2 应用场景

- 多个微服务之间共享用户认证状态
- 不同域名的应用之间实现单点登录
- 第三方系统接入统一认证中心
- 应用间跳转登录：B应用跳转至A应用（认证中心）登录后返回B应用

## 2. 系统架构

### 2.1 架构图

```
+----------------+       +----------------+       +----------------+
|                |       |                |       |                |
|  应用A         +-----> |  SSO认证中心   +-----> |  应用B         |
|  (客户端)       |       |  (Historical  |       |  (客户端)       |
|                | <-----+   Reading)    | <-----+                |
+----------------+       +----------------+       +----------------+
                               ^
                               |
                        +------+------+
                        |             |
                        |  JWT存储    |
                        |  (Token)    |
                        +-------------+
```

### 2.2 核心组件

1. **认证中心（Auth Center）**
   - 负责用户身份验证和JWT令牌生成
   - 提供登录、注册等基本认证功能
   - 提供单点登录特定的接口

2. **SSO服务（SsoService）**
   - 处理单点登录相关的业务逻辑
   - 提供令牌验证、用户信息获取等服务

3. **JWT提供者（JwtTokenProvider）**
   - 处理JWT令牌的生成、解析和验证
   - 确保令牌的安全性和有效性

4. **客户端应用（Client Applications）**
   - 第三方应用，需要集成单点登录功能
   - 通过调用SSO接口实现用户认证

## 3. 接口详细说明

### 3.1 认证接口（AuthController）

#### 3.1.1 单点登录重定向接口

**URL**: `/auth/login-with-redirect`
**方法**: `POST`
**参数**:
- `loginDto`: JSON对象，包含用户名和密码
  - `username`: 用户名
  - `password`: 密码
- `redirectUri`: 登录成功后的重定向URL

**返回值**: HTTP 302重定向，重定向URL中包含token参数

**功能描述**: 用户在第三方应用点击登录时，重定向到认证中心登录页面，登录成功后将JWT令牌作为URL参数重定向回原应用。

#### 3.1.2 令牌验证接口

**URL**: `/auth/validate-token`
**方法**: `GET`
**参数**: `token` - JWT令牌

**返回值**:
```json
{
  "code": 200,
  "data": true/false,
  "message": "成功"
}
```

**功能描述**: 验证JWT令牌是否有效，供其他服务调用。

#### 3.1.3 用户信息获取接口

**URL**: `/auth/user-info`
**方法**: `GET`
**参数**: `token` - JWT令牌

**返回值**:
```json
{
  "code": 200,
  "data": "用户名",
  "message": "成功"
}
```

**功能描述**: 根据JWT令牌获取用户信息，供其他服务调用。

### 3.2 SSO接口（SsoController）

#### 3.2.1 验证令牌有效性

**URL**: `/sso/validate`
**方法**: `GET`
**参数**: `token` - JWT令牌

**返回值**:
```json
{
  "code": 200,
  "data": true/false,
  "message": "成功"
}
```

**功能描述**: 验证JWT令牌是否有效，供其他服务调用。

#### 3.2.2 获取令牌中的用户信息

**URL**: `/sso/user-info`
**方法**: `GET`
**参数**: `token` - JWT令牌

**返回值**:
```json
{
  "code": 200,
  "data": {
    "username": "用户名"
  },
  "message": "成功"
}
```

**功能描述**: 获取JWT令牌中的用户信息，以Map形式返回，供其他服务调用。

#### 3.2.3 生成会话状态标识

**URL**: `/sso/generate-state`
**方法**: `GET`
**参数**: 无

**返回值**:
```json
{
  "code": 200,
  "data": "会话状态标识",
  "message": "成功"
}
```

**功能描述**: 生成唯一的会话状态标识，用于防止CSRF攻击。

#### 3.2.4 SSO登出接口

**URL**: `/sso/logout`
**方法**: `POST`
**参数**: `Authorization` - 请求头中的认证信息（可选）

**返回值**:
```json
{
  "code": 200,
  "data": true,
  "message": "成功"
}
```

**功能描述**: 单点登录登出接口，在无状态系统中主要由客户端负责清除token。

#### 3.2.5 检查单点登录状态

**URL**: `/sso/check-status`
**方法**: `GET`
**参数**: `token` - JWT令牌

**返回值**:
```json
{
  "code": 200,
  "data": {
    "isAuthenticated": true/false,
    "username": "用户名" // 仅当isAuthenticated为true时存在
  },
  "message": "成功"
}
```

**功能描述**: 验证当前token的有效性并返回基本信息。

## 4. 单点登录流程

### 4.1 登录流程

1. **客户端重定向到SSO认证中心**
   - 客户端应用检测到用户未登录时，重定向到认证中心的登录页面
   - 必须携带`redirectUri`参数，指定登录成功后的跳转地址
   - 重定向URL格式：`https://sso-server.example.com/login?redirectUri=https://client-app.example.com/callback`

2. **用户输入凭据并登录**
   - 用户在认证中心输入用户名和密码
   - 认证中心登录页面自动检测URL中的`redirectUri`参数
   - 点击登录按钮，如果存在`redirectUri`参数，调用`/auth/login-with-redirect`接口
   - 如果不存在`redirectUri`参数，则调用普通的`/auth/login`接口

3. **认证中心验证凭据并生成令牌**
   - 认证中心验证用户凭据是否正确
   - 验证通过后，生成JWT令牌

4. **重定向回客户端应用**
   - 认证中心将JWT令牌作为URL参数，重定向回客户端应用
   - 重定向URL格式：`redirectUri?token=jwt_token_value`

5. **客户端处理令牌**
   - 客户端应用从URL参数中提取JWT令牌
   - 将令牌存储在本地（如localStorage）
   - 后续请求携带令牌进行身份验证

### 4.3 应用间跳转登录使用方法详解

#### 4.3.1 第三方应用（B应用）配置步骤

1. **检测用户登录状态**
   ```javascript
   function checkUserLogin() {
     const token = localStorage.getItem('sso_token');
     if (!token) {
       // 用户未登录，重定向到SSO认证中心
       redirectToSsoLogin();
       return false;
     }
     // 验证token有效性
     return validateToken(token);
   }
   ```

2. **重定向到SSO认证中心**
   ```javascript
   function redirectToSsoLogin() {
     // 编码当前页面URL作为重定向目标
     const redirectUri = encodeURIComponent(window.location.href);
     // 重定向到SSO登录页面，携带redirectUri参数
     window.location.href = 'https://sso-server.example.com/login?redirectUri=' + redirectUri;
   }
   ```

3. **处理SSO登录成功后的回调**
   ```javascript
   function handleSsoCallback() {
     // 从URL参数中获取token
     const urlParams = new URLSearchParams(window.location.search);
     const token = urlParams.get('token');
     
     if (token) {
       // 存储token到本地存储
       localStorage.setItem('sso_token', token);
       // 清理URL中的token参数
       const cleanUrl = window.location.origin + window.location.pathname + window.location.hash;
       window.history.replaceState({}, document.title, cleanUrl);
       
       // 可以选择验证token并获取用户信息
       validateAndFetchUserInfo(token);
       return true;
     }
     return false;
   }
   ```

4. **页面加载时初始化SSO流程**
   ```javascript
   document.addEventListener('DOMContentLoaded', function() {
     // 首先检查是否是SSO回调（包含token参数）
     if (handleSsoCallback()) {
       // 已经处理了SSO回调，用户已登录
       console.log('用户已通过SSO登录');
     } else {
       // 不是回调，检查用户登录状态
       checkUserLogin();
     }
   });
   ```

#### 4.3.2 SSO认证中心（A应用）工作原理

1. **登录页面URL参数处理**
   - 登录页面（login.html）自动检测URL中的`redirectUri`参数
   - 登录表单提交时，根据是否存在`redirectUri`参数选择调用不同的API

2. **API调用逻辑**
   - 当存在`redirectUri`时，调用`/auth/login-with-redirect`接口
   - 当不存在`redirectUri`时，调用普通的`/auth/login`接口

3. **重定向实现**
   - `/auth/login-with-redirect`接口在验证成功后，生成JWT令牌并构建重定向URL
   - 返回HTTP 302响应，将用户浏览器重定向回第三方应用，并在URL中携带token

#### 4.3.3 完整示例：B应用跳转到A应用登录后返回

**B应用代码（跳转登录）：**
```javascript
// B应用中的登录检查逻辑
function initSso() {
  // 检查URL中是否已有token（SSO回调）
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('token');
  
  if (token) {
    // 处理SSO回调，保存token
    localStorage.setItem('sso_token', token);
    // 清除URL中的token参数
    const cleanUrl = window.location.origin + window.location.pathname;
    window.history.replaceState({}, document.title, cleanUrl);
    
    // 验证token并获取用户信息
    fetchUserInfo(token);
  } else {
    // 检查本地是否已有token
    const storedToken = localStorage.getItem('sso_token');
    if (!storedToken) {
      // 未登录，重定向到SSO认证中心
      const redirectUri = encodeURIComponent(window.location.href);
      window.location.href = 'https://sso-server.example.com/login?redirectUri=' + redirectUri;
    } else {
      // 验证现有token
      validateToken(storedToken);
    }
  }
}

// 获取用户信息
async function fetchUserInfo(token) {
  try {
    const response = await fetch('https://sso-server.example.com/auth/user-info?token=' + token);
    const data = await response.json();
    if (data.code === 200) {
      // 显示用户信息
      document.getElementById('username').textContent = data.data;
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
}

// 页面加载时初始化
window.onload = initSso;
```

**A应用（SSO认证中心）登录页面代码片段：**
```javascript
// 从URL中获取redirectUri参数
function getQueryParam(name) {
  const urlParams = new URLSearchParams(window.location.search);
  return urlParams.get(name);
}

// 表单提交处理
async function handleLogin(event) {
  event.preventDefault();
  
  const username = document.getElementById('username').value;
  const password = document.getElementById('password').value;
  const redirectUri = getQueryParam('redirectUri');
  
  try {
    let response;
    if (redirectUri) {
      // 使用带重定向的登录接口
      response = await fetch('/auth/login-with-redirect', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password, redirectUri })
      });
      
      if (response.redirected) {
        // 跟随重定向到第三方应用
        window.location.href = response.url;
        return;
      }
    } else {
      // 使用普通登录接口
      response = await fetch('/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
      });
    }
    
    const data = await response.json();
    // 处理普通登录响应...
  } catch (error) {
    console.error('登录失败:', error);
  }
}
```

### 4.2 验证流程

1. **客户端请求携带令牌**
   - 客户端应用在请求头中携带JWT令牌
   - 请求头格式：`Authorization: Bearer jwt_token_value`

2. **服务端验证令牌**
   - 服务端应用收到请求后，提取令牌
   - 调用SSO认证中心的`/sso/validate`或`/auth/validate-token`接口验证令牌

3. **获取用户信息**
   - 令牌验证通过后，服务端可以调用`/sso/user-info`或`/auth/user-info`接口获取用户信息
   - 根据用户信息进行权限控制和业务处理

## 5. 客户端集成示例

### 5.1 JavaScript集成示例

```javascript
// 检查用户是否已登录
function checkLogin() {
    // 从localStorage获取token
    const token = localStorage.getItem('sso_token');
    
    if (!token) {
        // 未登录，跳转到SSO登录页面
        const redirectUri = encodeURIComponent(window.location.href);
        window.location.href = 'https://sso-server.example.com/login?redirect_uri=' + redirectUri;
        return false;
    }
    
    // 验证token有效性
    return validateToken(token);
}

// 验证token
async function validateToken(token) {
    try {
        const response = await fetch('https://sso-server.example.com/sso/validate?token=' + token);
        const result = await response.json();
        
        if (result.code === 200 && result.data) {
            return true;
        } else {
            // token无效，清除并跳转到登录页
            localStorage.removeItem('sso_token');
            const redirectUri = encodeURIComponent(window.location.href);
            window.location.href = 'https://sso-server.example.com/login?redirect_uri=' + redirectUri;
            return false;
        }
    } catch (error) {
        console.error('验证token失败:', error);
        return false;
    }
}

// 处理SSO重定向回来的token
function handleSsoRedirect() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    
    if (token) {
        // 存储token
        localStorage.setItem('sso_token', token);
        // 移除URL中的token参数，避免重复使用
        const cleanUrl = window.location.origin + window.location.pathname + window.location.hash;
        window.history.replaceState({}, document.title, cleanUrl);
        return true;
    }
    
    return false;
}

// 登出
function logout() {
    localStorage.removeItem('sso_token');
    // 可以选择调用SSO服务的登出接口
    fetch('https://sso-server.example.com/sso/logout', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('sso_token')
        }
    }).then(() => {
        // 登出成功，可以跳转到登录页或其他页面
        window.location.href = '/';
    });
}

// 初始化
function init() {
    // 检查是否是从SSO重定向回来的
    const isFromSso = handleSsoRedirect();
    
    if (!isFromSso) {
        // 不是从SSO重定向回来，检查登录状态
        checkLogin();
    }
}

// 页面加载时执行
window.onload = init;
```

### 5.2 服务器端集成示例（Java）

```java
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class SsoClient {
    private final String ssoServerUrl;
    private final RestTemplate restTemplate;
    
    public SsoClient(String ssoServerUrl) {
        this.ssoServerUrl = ssoServerUrl;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 验证JWT令牌
     */
    public boolean validateToken(String token) {
        try {
            String url = ssoServerUrl + "/sso/validate?token=" + token;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return (boolean) body.getOrDefault("data", false);
            }
            
            return false;
        } catch (Exception e) {
            // 记录异常
            System.err.println("验证token失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取用户信息
     */
    public Map<String, Object> getUserInfo(String token) {
        try {
            String url = ssoServerUrl + "/sso/user-info?token=" + token;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return (Map<String, Object>) body.getOrDefault("data", new HashMap<>());
            }
            
            return new HashMap<>();
        } catch (Exception e) {
            // 记录异常
            System.err.println("获取用户信息失败: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 生成重定向到SSO登录页面的URL
     */
    public String generateLoginUrl(String redirectUri) {
        return ssoServerUrl + "/login?redirect_uri=" + encodeUrl(redirectUri);
    }
    
    private String encodeUrl(String url) {
        try {
            return java.net.URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return url;
        }
    }
}
```

## 6. 安全性考虑

### 6.1 安全最佳实践

1. **使用HTTPS**
   - 确保所有SSO相关的通信都通过HTTPS进行，防止中间人攻击

2. **令牌安全存储**
   - 客户端应用应将JWT令牌存储在安全的地方（如localStorage或HttpOnly Cookie）
   - 避免在URL中明文传递令牌（除了必要的重定向场景）
   - **重要**：使用重定向方式传递token后，客户端应用必须尽快从URL中移除token参数

3. **令牌过期时间**
   - 设置合理的JWT令牌过期时间，建议不超过24小时
   - 考虑实现令牌刷新机制，提高用户体验

4. **CSRF防护**
   - 使用会话状态标识（state）防止CSRF攻击
   - 在重定向登录流程中验证state参数
   - 建议在重定向URL中添加state参数：`?redirectUri=...&state=random_value`

5. **输入验证**
   - 对所有用户输入和URL参数进行严格验证
   - 避免重定向到不安全的外部域名
   - **关键安全措施**：SSO服务器应验证重定向URI是否在允许的域名白名单中

### 6.2 潜在风险及防范

1. **令牌泄露**
   - 风险：JWT令牌如果被截获，攻击者可以冒充用户身份
   - 防范：使用HTTPS，设置合理的过期时间，考虑实现令牌撤销机制

2. **重定向攻击**
   - 风险：恶意网站可能利用重定向机制诱导用户访问钓鱼网站
   - 防范：验证重定向URL是否在允许的域名白名单中

3. **跨站脚本攻击（XSS）**
   - 风险：如果客户端应用存在XSS漏洞，存储的令牌可能被窃取
   - 防范：使用HttpOnly Cookie存储令牌，实施内容安全策略（CSP）

## 7. 故障排除

### 7.1 常见问题

1. **令牌验证失败**
   - 检查令牌是否过期
   - 验证令牌格式是否正确
   - 确认使用的密钥是否匹配

2. **重定向循环**
   - 检查重定向URI是否正确配置
   - 确认客户端应用是否正确处理令牌

3. **跨域请求被拒绝**
   - 验证CORS配置是否允许目标域名
   - 检查请求头中的Origin信息

### 7.2 日志和监控

- 建议在SSO服务端实现详细的日志记录，包括登录尝试、令牌验证、错误信息等
- 考虑实现监控机制，及时发现异常访问模式

## 8. 维护和更新

### 8.1 JWT密钥轮换

- 定期轮换JWT签名密钥，增强安全性
- 实施密钥轮换策略，确保平滑过渡

### 8.2 升级注意事项

- 升级JWT库或Spring Security时，注意版本兼容性
- 更新认证逻辑时，确保向后兼容现有令牌

## 9. 联系和支持

如有任何关于单点登录系统的问题或建议，请联系系统管理员。

---

文档版本：1.0
更新日期：2024年
