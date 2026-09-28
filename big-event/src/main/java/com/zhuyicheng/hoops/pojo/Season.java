package com.zhuyicheng.hoops.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Season {
    private Integer id; // 主键ID

    @NotEmpty(message = "赛季名不能为空")
    @Size(max = 20, message = "赛季名长度不能超过20个字符")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "赛季名格式必须为 2025-26 这样的年份格式")
    private String name; // 赛季名，如 2025-26

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate; // 开始日期

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate; // 结束日期

    @NotNull(message = "是否当前赛季不能为空")
    private Integer isCurrent; // 是否当前赛季：1是 0否

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 修改时间
}
