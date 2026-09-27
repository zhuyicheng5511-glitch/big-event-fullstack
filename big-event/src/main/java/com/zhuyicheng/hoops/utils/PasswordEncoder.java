package com.zhuyicheng.hoops.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 密码编码器。
 *
 * <p>新密码统一使用 <b>BCrypt</b>：自带随机盐、可调工作因子，
 * 同一个明文每次加密结果都不同，彩虹表与「撞库」手段失效。
 *
 * <p>同时兼容历史上用<b>无盐 MD5</b> 存储的旧数据：
 * 校验时能识别 32 位十六进制格式并走旧的 MD5 比对逻辑，
 * 由调用方在登录成功后触发一次静默升级（见 {@link #needsUpgrade}）。
 * 这样换算法不需要让所有老用户重置密码。
 */
@Component
public class PasswordEncoder {

    /** 32 位十六进制 = 旧的 MD5 哈希；BCrypt 哈希以 $2a$/$2b$ 开头，两者不会混淆 */
    private static final Pattern LEGACY_MD5 = Pattern.compile("^[0-9a-fA-F]{32}$");

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    /** 加密明文密码 */
    public String encode(String rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    /**
     * 校验密码，兼容 BCrypt 与历史 MD5 两种存量格式。
     *
     * @param rawPassword 用户提交的明文
     * @param storedHash  数据库中存储的哈希
     */
    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        if (isLegacyMd5(storedHash)) {
            return storedHash.equalsIgnoreCase(md5(rawPassword));
        }
        return bcrypt.matches(rawPassword, storedHash);
    }

    /** 该哈希是否需要升级为 BCrypt（即：是否还是历史 MD5） */
    public boolean needsUpgrade(String storedHash) {
        return storedHash != null && isLegacyMd5(storedHash);
    }

    private boolean isLegacyMd5(String hash) {
        return LEGACY_MD5.matcher(hash).matches();
    }

    private String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }
}
