package com.example.bigevent.interceptors;

import com.example.bigevent.utils.JwtUtil;
import com.example.bigevent.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Map;

@Component // 必须声明为 Spring 容器管理的组件
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate; // 1. 注入 Redis 模板工具

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");

        try {
            // 2. === 先从 Redis 中获取这个 Token ===
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(token);

            // 3. === 如果 Redis 中不存在这个 Token，说明用户已经退出了登录，直接拦截 ===
            if (redisToken == null) {
                throw new RuntimeException(); // 抛出异常，进入 catch 块返回 401
            }

            Map<String, Object> claims = JwtUtil.parseToken(token);
            ThreadLocalUtil.set(claims);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 一次请求处理完毕后，必须清空 ThreadLocal 中的数据，否则会导致严重的内存泄漏
        ThreadLocalUtil.remove();
    }
}