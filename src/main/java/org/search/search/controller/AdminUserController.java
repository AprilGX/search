package org.search.search.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.search.search.entity.SysUser;
import org.search.search.mapper.SysUserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/search/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    @RequestParam(required = false) String username) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> result = sysUserMapper.selectPage(pageParam, wrapper);

        // 脱敏处理：不返回密码
        result.getRecords().forEach(u -> u.setPassword(null));

        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("data", result);
        return map;
    }

    /**
     * 新增用户
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody SysUser user) {
        Map<String, Object> map = new HashMap<>();
        
        // 查重
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            map.put("code", 400);
            map.put("message", "用户名已存在");
            return map;
        }

        user.setCreateTime(LocalDateTime.now());
        // 如果前端传了SHA256哈希后的密码，这里再次加密
        // 如果没传密码，设置默认密码 (这里假设前端传的是已经哈希过的或明文，为了统一，我们统一加密)
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            // 默认密码逻辑可以在这里写，或者前端传过来
            map.put("code", 400);
            map.put("message", "必须设置密码");
            return map;
        }
        
        // 注意：AdminLoginService里是用matches(SHA256, BCrypt)
        // 所以入库必须是 BCrypt 加密后的
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        if (user.getRole() == null) user.setRole("ADMIN");
        if (user.getStatus() == null) user.setStatus(1);

        sysUserMapper.insert(user);

        map.put("code", 200);
        map.put("message", "创建成功");
        return map;
    }

    /**
     * 更新状态 (启用/禁用)
     */
    @PostMapping("/update-status")
    public Map<String, Object> updateStatus(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        Integer status = Integer.valueOf(body.get("status").toString());
        
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        sysUserMapper.updateById(user);
        
        return Map.of("code", 200, "message", "状态更新成功");
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    public Map<String, Object> delete(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        sysUserMapper.deleteById(id);
        return Map.of("code", 200, "message", "删除成功");
    }
}
