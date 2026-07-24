package com.example.bigevent.service;

import com.example.bigevent.pojo.User;

public interface UserService {
    // 根据用户名查询用户
    User findByUsername(String username);

    // 注册新用户
    void register(String username, String password);

    // 更新用户头像
    void updateAvatar(String avatarUrl);

    // 更新用户密码
    void updatePassword(String newPassword);

    // 更新用户信息
    void update(User user);
}