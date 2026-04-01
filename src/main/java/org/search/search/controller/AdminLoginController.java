package org.search.search.controller;

import org.search.search.service.AdminLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/search/admin")
public class AdminLoginController {

    @Autowired
    private AdminLoginService adminLoginService;

    // 临时工具接口，生成 BCrypt 密码
    @GetMapping("/gen-pass")
    public String genPass(@RequestParam String password) {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        Map<String, Object> response = new HashMap<>();
        try {
            String token = adminLoginService.login(username, password);
            response.put("code", 200);
            response.put("token", token);
            response.put("message", "登录成功");
        } catch (Exception e) {
            response.put("code", 401);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/update-password")
    public Map<String, Object> updatePassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        Map<String, Object> response = new HashMap<>();
        try {
            adminLoginService.updatePassword(username, oldPassword, newPassword);
            response.put("code", 200);
            response.put("message", "密码修改成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/update-username")
    public Map<String, Object> updateUsername(@RequestBody Map<String, String> request) {
        String currentUsername = request.get("currentUsername");
        String newUsername = request.get("newUsername");

        Map<String, Object> response = new HashMap<>();
        try {
            adminLoginService.updateUsername(currentUsername, newUsername);
            response.put("code", 200);
            response.put("message", "用户名修改成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
