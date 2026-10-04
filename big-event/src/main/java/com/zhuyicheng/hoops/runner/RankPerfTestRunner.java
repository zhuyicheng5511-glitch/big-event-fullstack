package com.zhuyicheng.hoops.runner;

import com.zhuyicheng.hoops.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class RankPerfTestRunner implements CommandLineRunner {

    @Autowired
    private RankService rankService;

    @Override
    public void run(String... args) throws Exception {
        if (!shouldRun(args)) {
            return;
        }

        Integer seasonId = 1;
        Integer teamId = null;
        String position = null;
        Integer limit = 10;
        int iterations = 100;

        List<Long> noCacheTimes = new ArrayList<>(iterations);
        for (int i = 0; i < iterations; i++) {
            // 关键：清缓存必须放在循环【内部】。
            // 若放在循环外，只有第 1 次是真正回源，第 2~iterations 次都会命中
            // 上一次写入的缓存，于是 P95 测到的其实是缓存耗时，两组数据失去对比意义。
            // 统一走 RankService 的 SCAN 清缓存实现，避免此处再用阻塞的 KEYS 命令。
            rankService.evictPointsRankCache();

            long start = System.nanoTime();
            rankService.pointsRank(seasonId, teamId, position, limit);
            long elapsed = System.nanoTime() - start;
            noCacheTimes.add(elapsed / 1_000_000); // ns -> ms
        }

        // 2. 再次测试，此时缓存已由上一步最后一次调用写入
        List<Long> cacheTimes = new ArrayList<>(iterations);
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            rankService.pointsRank(seasonId, teamId, position, limit);
            long elapsed = System.nanoTime() - start;
            cacheTimes.add(elapsed / 1_000_000);
        }

        System.out.println("[RankPerfTest] 无缓存 P95: " + p95(noCacheTimes) + " ms");
        System.out.println("[RankPerfTest] 有缓存 P95: " + p95(cacheTimes) + " ms");
    }

    private long p95(List<Long> times) {
        List<Long> sorted = new ArrayList<>(times);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
        return sorted.get(Math.max(0, index));
    }

    private boolean shouldRun(String[] args) {
        if (args == null) return false;
        for (String arg : args) {
            if ("--perf-test".equals(arg)) {
                return true;
            }
        }
        return false;
    }
}
