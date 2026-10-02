package com.zhuyicheng.hoops.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class StatCalculator {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal HALF = BigDecimal.valueOf(0.5);
    private static final BigDecimal TS_FTA_COEFF = BigDecimal.valueOf(0.44);

    private StatCalculator() {
    }

    public static Double fgPct(Integer fgm, Integer fga) {
        if (fga == null || fga == 0) return null;
        return divide(fgm, fga);
    }

    public static Double threePPct(Integer fg3m, Integer fg3a) {
        if (fg3a == null || fg3a == 0) return null;
        return divide(fg3m, fg3a);
    }

    public static Double ftPct(Integer ftm, Integer fta) {
        if (fta == null || fta == 0) return null;
        return divide(ftm, fta);
    }

    // eFG% = (FGM + 0.5 * FG3M) / FGA
    public static Double efgPct(Integer fgm, Integer fg3m, Integer fga) {
        if (fga == null || fga == 0) return null;
        BigDecimal numerator = BigDecimal.valueOf(fgm).add(BigDecimal.valueOf(fg3m).multiply(HALF));
        return numerator.divide(BigDecimal.valueOf(fga), 4, RoundingMode.HALF_UP).doubleValue();
    }

    // TS% = PTS / (2 * (FGA + 0.44 * FTA))
    public static Double tsPct(Integer pts, Integer fga, Integer fta) {
        if (fga == null || fta == null || (fga == 0 && fta == 0)) return null;
        BigDecimal denom = TWO.multiply(BigDecimal.valueOf(fga).add(TS_FTA_COEFF.multiply(BigDecimal.valueOf(fta))));
        return BigDecimal.valueOf(pts).divide(denom, 4, RoundingMode.HALF_UP).doubleValue();
    }

    // EFF = (PTS+REB+AST+STL+BLK) - (FGA-FGM) - (FTA-FTM) - TOV
    public static Integer eff(Integer pts, Integer reb, Integer ast, Integer stl, Integer blk,
                                Integer fga, Integer fgm, Integer fta, Integer ftm, Integer tov) {
        if (pts == null || reb == null || ast == null || stl == null || blk == null
                || fga == null || fgm == null || fta == null || ftm == null || tov == null) {
            return null;
        }
        return pts + reb + ast + stl + blk - (fga - fgm) - (fta - ftm) - tov;
    }

    public static Double per36(Double stat, Double minutes) {
        if (minutes == null || minutes == 0) return null;
        return BigDecimal.valueOf(stat).multiply(BigDecimal.valueOf(36))
                .divide(BigDecimal.valueOf(minutes), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // 统计 >=10 的分项个数，返回 2=两双, 3=三双
    public static Integer doubleDoubleCount(Integer pts, Integer reb, Integer ast, Integer stl, Integer blk) {
        int count = 0;
        if (pts != null && pts >= 10) count++;
        if (reb != null && reb >= 10) count++;
        if (ast != null && ast >= 10) count++;
        if (stl != null && stl >= 10) count++;
        if (blk != null && blk >= 10) count++;
        return count;
    }

    private static Double divide(Integer a, Integer b) {
        if (a == null || b == null || b == 0) return null;
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), 4, RoundingMode.HALF_UP).doubleValue();
    }
}
