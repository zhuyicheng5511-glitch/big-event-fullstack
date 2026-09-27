package com.zhuyicheng.hoops.service.impl;

import com.zhuyicheng.hoops.mapper.UserMapper;
import com.zhuyicheng.hoops.pojo.User;
import com.zhuyicheng.hoops.service.UserService;
import com.zhuyicheng.hoops.utils.PasswordEncoder;
import com.zhuyicheng.hoops.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public void register(String username, String password) {
        // 使用 BCrypt 加密（自带随机盐），不再使用无盐 MD5
        userMapper.add(username, passwordEncoder.encode(password));
    }

    @Override
    public void updateAvatar(String avatarUrl) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");
        userMapper.updateAvatar(avatarUrl, id);
    }

    @Override
    public void updatePassword(String newPassword) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");
        userMapper.updatePassword(passwordEncoder.encode(newPassword), id);
    }

    @Override
    public void updatePasswordById(Integer userId, String rawPassword) {
        userMapper.updatePassword(passwordEncoder.encode(rawPassword), userId);
    }

    @Override
    public void update(User user) {
        userMapper.update(user);
    }
}
