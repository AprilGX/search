package org.search.search.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.search.search.entity.SysUser;
import org.search.search.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminLoginService {

    @Autowired
    private SysUserMapper sysUserMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    // 密钥，生产环境应放在配置文件中
    private static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String login(String username, String password) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null) {
            System.out.println(">>> [Login Debug] 用户名不存在: " + username);
            throw new RuntimeException("用户不存在");
        }
        
        System.out.println(">>> [Login Debug] 找到用户: ID=" + user.getId() + ", 用户名=" + user.getUsername());

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        // 生成 JWT
        return Jwts.builder()
                .setSubject(username)
                .claim("role", user.getRole())
                .claim("userId", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24小时过期
                .signWith(key)
                .compact();
    }

    public void updatePassword(String username, String oldPassword, String newPassword) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        // 更新新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
    }

    public void updateUsername(String currentUsername, String newUsername) {
        // 1. 检查新用户名是否已存在
        SysUser existUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, newUsername));
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 获取当前用户
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, currentUsername));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 3. 更新
        user.setUsername(newUsername);
        sysUserMapper.updateById(user);
    }
}
