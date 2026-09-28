package com.zhuyicheng.hoops.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Team {
    private Integer id; // 主键ID

    @NotEmpty(message = "球队名不能为空")
    @Size(max = 50, message = "球队名长度不能超过50个字符")
    private String name; // 球队名，如 洛杉矶湖人

    @Size(max = 80, message = "英文名长度不能超过80个字符")
    private String nameEn; // 英文名，如 Lakers

    @Size(max = 50, message = "城市长度不能超过50个字符")
    private String city; // 城市

    @Size(max = 10, message = "联盟长度不能超过10个字符")
    private String conference; // 东部/西部

    @Size(max = 20, message = "分区长度不能超过20个字符")
    private String division; // 分区，如 太平洋区

    @Size(max = 255, message = "队标地址长度不能超过255个字符")
    private String logoUrl; // 队标地址

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 修改时间
}
