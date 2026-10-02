package com.zhuyicheng.hoops.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneStat {
    private String zone;
    private Long attempts;
    private Long made;
    private BigDecimal fgPct;
    private Double centerX;
    private Double centerY;
}
