package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.User;

public interface UserService {
    // 根据用户名查询用户
    User findByUsername(String username);

    // 注册新用户
    void register(String username, String password);

    // 更新用户头像
    void updateAvatar(String avatarUrl);

    // 更新用户密码（作用于当前登录用户）
    void updatePassword(String newPassword);

    /**
     * 按指定用户 id 更新密码。
     * 用于登录时把历史 MD5 哈希静默升级为 BCrypt —— 此时请求走的是免鉴权的登录接口，
     * ThreadLocal 中没有当前用户，所以必须显式传入 id。
     */
    void updatePasswordById(Integer userId, String rawPassword);

    // 更新用户信息
    void update(User user);
}