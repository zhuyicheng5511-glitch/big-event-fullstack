package com.zhuyicheng.hoops.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Game {
    private Integer id; // 主键ID

    @NotNull(message = "赛季ID不能为空")
    private Integer seasonId; // 赛季ID

    @NotNull(message = "主队ID不能为空")
    private Integer homeTeamId; // 主队ID

    @NotNull(message = "客队ID不能为空")
    private Integer awayTeamId; // 客队ID

    @NotNull(message = "比赛时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gameDate; // 比赛时间

    private Integer homeScore; // 主队得分

    private Integer awayScore; // 客队得分

    @NotEmpty(message = "比赛状态不能为空")
    @Size(max = 10, message = "比赛状态长度不能超过10个字符")
    private String status; // 未开始/进行中/已结束

    @Size(max = 80, message = "场馆长度不能超过80个字符")
    private String venue; // 比赛场馆

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 修改时间
}
