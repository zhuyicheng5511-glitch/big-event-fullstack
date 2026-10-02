package com.zhuyicheng.hoops.pojo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Shot {
    private Long id; // 主键ID

    @NotNull(message = "比赛ID不能为空")
    private Integer gameId; // 比赛ID

    @NotNull(message = "球员ID不能为空")
    private Integer playerId; // 球员ID

    @NotNull(message = "节次不能为空")
    private Integer quarter; // 节次：1-4，加时 5+

    private String gameClock; // 本节剩余时间 mm:ss

    @NotNull(message = "横坐标不能为空")
    private BigDecimal x; // 横坐标(英尺) 0-50

    @NotNull(message = "纵坐标不能为空")
    private BigDecimal y; // 纵坐标(英尺) 0-47，0为底线

    private BigDecimal distanceFt; // 出手距离(英尺)

    @NotEmpty(message = "投篮类型不能为空")
    private String shotType; // 两分/三分/罚球

    @NotNull(message = "是否命中不能为空")
    private Integer made; // 是否命中：1是 0否

    @NotEmpty(message = "区域不能为空")
    private String zone; // 8 区域之一

    private Integer assistedBy; // 助攻人球员ID

    private LocalDateTime createTime; // 创建时间（shot 表无 update_time）
}
