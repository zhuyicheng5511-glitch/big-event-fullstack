package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    // 根据用户名查询用户
    @Select("select * from user where username = #{username}")
    User findByUsername(String username);

    // 注册新用户
    @Insert("insert into user(username, password, create_time, update_time) " +
            "values(#{username}, #{password}, now(), now())")
    void add(String username, String password);

    @Update("update user set user_pic = #{avatarUrl}, update_time = now() where id = #{id}")
    void updateAvatar(String avatarUrl, Integer id);

    @Update("update user set password = #{newPassword}, update_time = now() where id = #{id}")
    void updatePassword(String newPassword, Integer id);

    @Update("update user set nickname = #{nickname}, email = #{email}, update_time = now() where id = #{id}")
    void update(User user);
}