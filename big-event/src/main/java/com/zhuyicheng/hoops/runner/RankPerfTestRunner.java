package com.zhuyicheng.hoops.runner;

import com.zhuyicheng.hoops.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class RankPerfTestRunner implements CommandLineRunner {

    @Autowired
    private RankService rankService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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

        // 1. 清空缓存，测试无缓存
        stringRedisTemplate.keys("rank:points:*").forEach(stringRedisTemplate::delete);

        List<Long> noCacheTimes = new ArrayList<>(iterations);
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            rankService.pointsRank(seasonId, teamId, position, limit);
            long elapsed = System.nanoTime() - start;
            noCacheTimes.add(elapsed / 1_000_000); // ns -> ms
        }

        // 2. 再次测试，此时缓存已存在
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
