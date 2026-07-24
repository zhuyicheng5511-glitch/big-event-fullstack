package com.example.bigevent.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Integer id;
    private String username;

    @JsonIgnore // 让 Spring MVC 在转 JSON 时忽略密码，保障安全
    private String password;

    private String nickname;
    private String email;
    private String userPic; // 用户头像地址
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}