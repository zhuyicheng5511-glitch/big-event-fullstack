package com.zhuyicheng.hoops.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类。
 *
 * <p>密钥与有效期全部来自配置（{@code jwt.secret} / {@code jwt.expire-hours}），
 * 不再硬编码在源码里 —— 硬编码的密钥会随源码一起进版本库，任何人都能据此刻伪造 token。
 *
 * <p>启动时校验密钥长度：HS256 要求至少 256 bit，配置不足直接启动失败，
 * 避免有人图省事配一个 "123456" 之类的弱密钥。
 */
@Component
public class JwtUtil {

    /** HS256 的最小密钥长度（字节） */
    private static final int MIN_SECRET_BYTES = 32;

    private final Algorithm algorithm;
    private final long expireHours;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire-hours}") long expireHours) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret 未配置，请在 application.yml 或环境变量 JWT_SECRET 中配置");
        }
        int length = secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret 长度不足：当前 " + length + " 字节，HS256 要求至少 " + MIN_SECRET_BYTES + " 字节");
        }
        if (expireHours <= 0) {
            throw new IllegalStateException("jwt.expire-hours 必须为正数，当前为 " + expireHours);
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.expireHours = expireHours;
    }

    /**
     * 生成 token。
     *
     * @param claims 载荷，放入 "claims" 字段下（与既有 token 结构保持一致）
     */
    public String genToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + expireHours * 60 * 60 * 1000))
                .sign(algorithm);
    }

    /**
     * 解析并校验 token。
     *
     * @throws com.auth0.jwt.exceptions.JWTVerificationException 签名不合法或已过期
     */
    public Map<String, Object> parseToken(String token) {
        return JWT.require(algorithm)
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }

    /**
     * token 有效期（小时）。
     * 供 Redis 会话记录的 TTL 复用，保证两处不会不一致。
     */
    public long getExpireHours() {
        return expireHours;
    }
}
