package com.zhuyicheng.hoops.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class StatCalculatorTest {

    @ParameterizedTest(name = "FG%: {0}/{1} -> {2}")
    @CsvSource({
            "10, 20, 0.5",
            "5, 12, 0.4167",
            "0, 5, 0.0",
    })
    @DisplayName("投篮命中率")
    void fgPct(Integer fgm, Integer fga, Double expected) {
        assertEquals(expected, StatCalculator.fgPct(fgm, fga), 0.0001);
    }

    @Test
    @DisplayName("投篮命中率除零返回 null")
    void fgPctZero() {
        assertNull(StatCalculator.fgPct(10, 0));
    }

    @ParameterizedTest(name = "3P%: {0}/{1} -> {2}")
    @CsvSource({
            "3, 7, 0.4286",
            "0, 5, 0.0",
    })
    @DisplayName("三分命中率")
    void threePPct(Integer fg3m, Integer fg3a, Double expected) {
        assertEquals(expected, StatCalculator.threePPct(fg3m, fg3a), 0.0001);
    }

    @Test
    @DisplayName("三分命中率除零返回 null")
    void threePPctZero() {
        assertNull(StatCalculator.threePPct(3, 0));
    }

    @ParameterizedTest(name = "eFG%: fgm={0}, fg3m={1}, fga={2} -> {3}")
    @CsvSource({
            "10, 3, 20, 0.575",
            "8, 0, 16, 0.5",
            "0, 0, 5, 0.0",
    })
    @DisplayName("有效命中率")
    void efgPct(Integer fgm, Integer fg3m, Integer fga, Double expected) {
        assertEquals(expected, StatCalculator.efgPct(fgm, fg3m, fga), 0.0001);
    }

    @Test
    @DisplayName("有效命中率除零返回 null")
    void efgPctZero() {
        assertNull(StatCalculator.efgPct(10, 3, 0));
    }

    @ParameterizedTest(name = "TS%: pts={0}, fga={1}, fta={2} -> {3}")
    @CsvSource({
            "28, 18, 6, 0.6783",
            "30, 21, 8, 0.6117",
            "20, 15, 0, 0.6667",
    })
    @DisplayName("真实命中率")
    void tsPct(Integer pts, Integer fga, Integer fta, Double expected) {
        assertEquals(expected, StatCalculator.tsPct(pts, fga, fta), 0.0001);
    }

    @Test
    @DisplayName("真实命中率与 Anthony Davis 样例核对")
    void tsPctSample() {
        // 30 分 / 21 出手 / 8 罚球 -> TS% 约 61.17%
        double ts = StatCalculator.tsPct(30, 21, 8);
        assertEquals(0.6117, ts, 0.0001);
    }

    @Test
    @DisplayName("真实命中率除零返回 null")
    void tsPctZero() {
        assertNull(StatCalculator.tsPct(20, 0, 0));
    }

    @ParameterizedTest(name = "EFF: {0}/{1}/{2}/{3}/{4} -> {5}")
    @CsvSource({
            "28, 8, 11, 2, 1, 38",
            "30, 14, 3, 1, 4, 40",
    })
    @DisplayName("效率值")
    void eff(Integer pts, Integer reb, Integer ast, Integer stl, Integer blk, Integer expected) {
        assertEquals(expected, StatCalculator.eff(pts, reb, ast, stl, blk, 18, 10, 6, 5, 3));
    }

    @ParameterizedTest(name = "per36: stat={0}, min={1} -> {2}")
    @CsvSource({
            "28, 35.5, 28.39",
            "0, 24.0, 0.0",
    })
    @DisplayName("每36分钟数据")
    void per36(Double stat, Double minutes, Double expected) {
        assertEquals(expected, StatCalculator.per36(stat, minutes), 0.01);
    }

    @Test
    @DisplayName("每36分钟除零返回 null")
    void per36Zero() {
        assertNull(StatCalculator.per36(20.0, 0.0));
    }

    @Test
    @DisplayName("两双/三双判定")
    void doubleDoubleCount() {
        assertEquals(2, StatCalculator.doubleDoubleCount(28, 8, 11, 2, 1));
        assertEquals(3, StatCalculator.doubleDoubleCount(20, 12, 10, 2, 1));
        assertEquals(0, StatCalculator.doubleDoubleCount(8, 5, 4, 1, 0));
    }

    @Test
    @DisplayName("除零边界不抛异常")
    void divisionByZeroDoesNotThrow() {
        assertDoesNotThrow(() -> StatCalculator.fgPct(10, 0));
        assertDoesNotThrow(() -> StatCalculator.threePPct(3, 0));
        assertDoesNotThrow(() -> StatCalculator.ftPct(5, 0));
        assertDoesNotThrow(() -> StatCalculator.efgPct(10, 3, 0));
        assertDoesNotThrow(() -> StatCalculator.tsPct(20, 0, 0));
        assertDoesNotThrow(() -> StatCalculator.per36(20.0, 0.0));
    }
}
