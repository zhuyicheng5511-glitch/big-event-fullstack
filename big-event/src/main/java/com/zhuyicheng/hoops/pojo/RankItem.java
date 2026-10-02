package com.zhuyicheng.hoops.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankItem {
    private Integer playerId;
    private String playerName;
    private String teamName;
    private String position;
    private Long totalPoints;
    private Long gamesPlayed;
    private BigDecimal avgPoints;
    private Long rank;
}
