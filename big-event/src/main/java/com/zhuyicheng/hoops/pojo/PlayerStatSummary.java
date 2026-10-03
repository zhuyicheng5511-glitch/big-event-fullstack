package com.zhuyicheng.hoops.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 球员维度的汇总统计（场均数据 + 命中率）。
 *
 * 存在的意义：明细分页接口单页最多 100 条，而一名球员一个赛季可能有几百场比赛，
 * 前端把"第一页 100 条"当全量去平均，算出来的场均数据是错的。
 * 这里把 COUNT / AVG / SUM 全部下推到数据库执行，无论多少场比赛都只返回一行，
 * 既保证结果正确，也不需要把明细传到前端再算。
 */
@Data
public class PlayerStatSummary {
    private Integer playerId;   // 球员ID
    private String playerName;  // 球员姓名
    private Integer games;      // 参与聚合的比赛场次

    private BigDecimal avgMinutes;   // 场均上场时间(分钟)
    private BigDecimal avgPoints;    // 场均得分
    private BigDecimal avgRebounds;  // 场均篮板
    private BigDecimal avgAssists;   // 场均助攻
    private BigDecimal avgSteals;    // 场均抢断
    private BigDecimal avgBlocks;    // 场均盖帽
    private BigDecimal avgTurnovers; // 场均失误

    private Integer totalPoints; // 总得分

    // 命中率必须用「总命中数 / 总出手数」计算，不能对各场命中率再取平均，
    // 否则一场 1 投 1 中和一场 10 投 5 中会被同等对待，结果偏高。
    private BigDecimal fgPct;  // 投篮命中率(%)
    private BigDecimal fg3Pct; // 三分命中率(%)
    private BigDecimal ftPct;  // 罚球命中率(%)
}
