package org.search.search.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器，处理网站根路径请求
 */
@Controller
public class HomeController {

    /**
     * 处理根路径请求，将请求转发到search.html页面
     * 需要认证后才能访问
     */
    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public String redirectToSearch() {
        // 使用forward:前缀保持URL不变，直接转发到search.html
        return "forward:/search.html";
    }
}