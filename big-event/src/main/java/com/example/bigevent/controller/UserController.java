package com.example.bigevent.controller;

import com.example.bigevent.pojo.Result;
import com.example.bigevent.pojo.User;
import com.example.bigevent.service.UserService;
import com.example.bigevent.utils.JwtUtil;
import com.example.bigevent.utils.Md5Util;
import com.example.bigevent.utils.ThreadLocalUtil;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Validated // 开启参数校验功能
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate; // 1. 注入 Redis 模板工具

    @PostMapping("/register")
    public Result register(
            @Pattern(regexp = "^\\S{5,16}$", message = "用户名必须是5-16位的非空字符") String username,
            @Pattern(regexp = "^\\S{5,16}$", message = "密码必须是5-16位的非空字符") String password
    ) {
        // 1. 查询用户名是否已被占用
        User u = userService.findByUsername(username);
        if (u == null) {
            // 用户名未被占用，允许注册
            userService.register(username, password);
            return Result.success();
        } else {
            // 用户名已被占用
            return Result.error("用户名已被占用");
        }
    }

    @PostMapping("/login")
    public Result login(
            @Pattern(regexp = "^\\S{5,16}$") String username,
            @Pattern(regexp = "^\\S{5,16}$") String password
    ) {
        User loginUser = userService.findByUsername(username);
        if (loginUser == null) {
            return Result.error("用户名错误");
        }

        if (Md5Util.getMD5(password).equals(loginUser.getPassword())) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", loginUser.getId());
            claims.put("username", loginUser.getUsername());
            String token = JwtUtil.genToken(claims);

            // 2. === 将 Token 存入 Redis，并设置过期时间为 12 小时（与 JWT 有效期一致） ===
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            operations.set(token, token, 12, TimeUnit.HOURS);

            return Result.success(token);
        }
        return Result.error("密码错误");
    }

    @GetMapping("/userInfo")
    public Result<User> userInfo() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        String username = (String) claims.get("username");

        User user = userService.findByUsername(username);
        return Result.success(user);
    }

    @PatchMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam String avatarUrl) {
        userService.updateAvatar(avatarUrl);
        return Result.success();
    }

    @PatchMapping("/updatePwd")
    public Result updatePassword(@RequestBody Map<String, String> params, @RequestHeader("Authorization") String token) {
        // 1. 校验参数（获取旧密码和新密码）
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");

        if (oldPwd == null || newPwd == null || rePwd == null || oldPwd.isEmpty() || newPwd.isEmpty() || rePwd.isEmpty()) {
            return Result.error("缺少必要参数");
        }

        // 2. 校验旧密码是否正确
        Map<String, Object> claims = ThreadLocalUtil.get();
        String username = (String) claims.get("username");
        User loginUser = userService.findByUsername(username);
        if (!loginUser.getPassword().equals(Md5Util.getMD5(oldPwd))) {
            return Result.error("原密码输入错误");
        }

        // 3. 校验新密码和确认密码是否一致
        if (!newPwd.equals(rePwd)) {
            return Result.error("两次输入的新密码不一致");
        }

        // 4. 修改数据库密码
        userService.updatePassword(newPwd);

        // 5. 核心：从 Redis 中删除该 Token，使其失效，强制下线！
        stringRedisTemplate.opsForValue().getOperations().delete(token);

        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody @Validated User user) {
        // 从 ThreadLocal 中获取真实的当前登录用户 ID
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");

        // 强制覆盖前端传过来的 ID，防止用户恶意篡改其他用户的数据
        user.setId(id);

        userService.update(user);
        return Result.success();
    }
}