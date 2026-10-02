package com.zhuyicheng.hoops.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PlayerGameStat {
    private Long id; // 主键ID

    @NotNull(message = "比赛ID不能为空")
    private Integer gameId; // 比赛ID

    @NotNull(message = "球员ID不能为空")
    private Integer playerId; // 球员ID

    @NotNull(message = "球队ID不能为空")
    private Integer teamId; // 所属球队ID

    @NotNull(message = "是否首发不能为空")
    private Integer isStarter; // 是否首发：1是 0否

    @NotNull(message = "上场时间不能为空")
    private BigDecimal minutes; // 上场时间(分钟)

    @NotNull(message = "得分不能为空")
    private Integer points; // 得分

    @NotNull(message = "篮板不能为空")
    private Integer rebounds; // 篮板

    @NotNull(message = "助攻不能为空")
    private Integer assists; // 助攻

    @NotNull(message = "抢断不能为空")
    private Integer steals; // 抢断

    @NotNull(message = "盖帽不能为空")
    private Integer blocks; // 盖帽

    @NotNull(message = "失误不能为空")
    private Integer turnovers; // 失误

    @NotNull(message = "犯规不能为空")
    private Integer fouls; // 犯规

    @NotNull(message = "投篮命中数不能为空")
    private Integer fgm; // 投篮命中数

    @NotNull(message = "投篮出手数不能为空")
    private Integer fga; // 投篮出手数

    @NotNull(message = "三分命中数不能为空")
    private Integer fg3m; // 三分命中数

    @NotNull(message = "三分出手数不能为空")
    private Integer fg3a; // 三分出手数

    @NotNull(message = "罚球命中数不能为空")
    private Integer ftm; // 罚球命中数

    @NotNull(message = "罚球出手数不能为空")
    private Integer fta; // 罚球出手数

    @NotNull(message = "正负值不能为空")
    private Integer plusMinus; // 正负值

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 修改时间
}
