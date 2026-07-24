package com.example.bigevent.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    // 密钥（用于加密和解密，请不要轻易泄露）
    private static final String KEY = "itheima";

    // 生成token(后台传入用户的id、username等基本信息)
    public static String genToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12)) // 12小时后过期
                .sign(Algorithm.HMAC256(KEY));
    }

    // 解析token, 获得载荷中的数据
    public static Map<String, Object> parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(KEY))
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }
}