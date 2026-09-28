package com.zhuyicheng.hoops.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Player {
    private Integer id; // 主键ID

    @NotEmpty(message = "球员中文名不能为空")
    @Size(max = 50, message = "球员中文名长度不能超过50个字符")
    private String name; // 中文名

    @Size(max = 80, message = "英文名长度不能超过80个字符")
    private String nameEn; // 英文名

    private Integer teamId; // 所属球队ID

    @Size(max = 5, message = "球衣号长度不能超过5个字符")
    private String jerseyNumber; // 球衣号

    @Size(max = 10, message = "位置长度不能超过10个字符")
    private String position; // 位置：PG/SG/SF/PF/C

    private Integer heightCm; // 身高(cm)

    private Integer weightKg; // 体重(kg)

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate; // 出生日期

    @Size(max = 30, message = "国籍长度不能超过30个字符")
    private String country; // 国籍

    @Size(max = 255, message = "头像地址长度不能超过255个字符")
    private String avatarUrl; // 头像地址

    @NotNull(message = "是否现役不能为空")
    private Integer isActive; // 是否现役：1是 0否

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 修改时间
}
