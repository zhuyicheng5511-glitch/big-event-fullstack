package com.zhuyicheng.hoops.runner;

import com.zhuyicheng.hoops.mapper.*;
import com.zhuyicheng.hoops.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
@Order(30) // 在 SeedDataRunner(10) 与 PlayerGameStatDataGeneratorRunner(20) 之后：投篮点要撒在已存在的比赛上
public class ShotDataGeneratorRunner implements CommandLineRunner {

    @Autowired
    private GameMapper gameMapper;
    @Autowired
    private PlayerMapper playerMapper;
    @Autowired
    private ShotMapper shotMapper;

    private final Random random = new Random(42);

    // zone -> (xMin, xMax, yMin, yMax, madeProbability)
    private static final Map<String, double[]> ZONES = new LinkedHashMap<>();

    static {
        ZONES.put("RIM", new double[]{22.0, 28.0, 0.0, 4.0, 0.60});
        ZONES.put("PAINT", new double[]{18.0, 32.0, 0.0, 14.0, 0.55});
        ZONES.put("MID", new double[]{10.0, 40.0, 8.0, 22.0, 0.45});
        ZONES.put("LEFT_CORNER_3", new double[]{0.0, 5.0, 0.0, 18.0, 0.35});
        ZONES.put("RIGHT_CORNER_3", new double[]{45.0, 50.0, 0.0, 18.0, 0.35});
        ZONES.put("LEFT_WING_3", new double[]{0.0, 15.0, 18.0, 35.0, 0.35});
        ZONES.put("RIGHT_WING_3", new double[]{35.0, 50.0, 18.0, 35.0, 0.35});
        ZONES.put("TOP_3", new double[]{20.0, 30.0, 22.0, 38.0, 0.34});
    }

    private static final List<String> ZONE_KEYS = new ArrayList<>(ZONES.keySet());
    private static final Map<String, String> ZONE_CN = new HashMap<>();
    private static final int[] ZONE_WEIGHTS = {35, 20, 15, 5, 5, 10, 10, 10};

    static {
        ZONE_CN.put("RIM", "篮下");
        ZONE_CN.put("PAINT", "油漆区");
        ZONE_CN.put("MID", "中距离");
        ZONE_CN.put("LEFT_CORNER_3", "左底角三分");
        ZONE_CN.put("RIGHT_CORNER_3", "右底角三分");
        ZONE_CN.put("LEFT_WING_3", "左翼三分");
        ZONE_CN.put("RIGHT_WING_3", "右翼三分");
        ZONE_CN.put("TOP_3", "弧顶三分");
    }

    @Override
    public void run(String... args) throws Exception {
        if (!shouldRun(args)) {
            return;
        }

        List<Game> games = gameMapper.list(null, null, null);
        List<Player> players = playerMapper.list(null, null);

        if (games.isEmpty() || players.isEmpty()) {
            System.out.println("[ShotDataGenerator] missing game or player data");
            return;
        }

        int target = 100_000;

        // 幂等：只补到 10 万，而不是每次无条件插 10 万。
        // compose 的 seed 服务在每次 docker compose up 都会重跑，原实现第二次会把
        // shot 叠加成 20 万，验收数字（count = 100000）直接对不上。
        long existing = shotMapper.count();
        if (existing >= target) {
            System.out.println("[ShotDataGenerator] shot 已有 " + existing + " 条，跳过生成");
            return;
        }
        target -= existing;

        int batchSize = 1000;
        long startTime = System.currentTimeMillis();
        List<Shot> batch = new ArrayList<>(batchSize);

        for (int i = 0; i < target; i++) {
            Game game = games.get(random.nextInt(games.size()));
            Player player = players.get(random.nextInt(players.size()));
            String zoneKey = pickZone();
            double[] range = ZONES.get(zoneKey);

            Shot shot = new Shot();
            shot.setGameId(game.getId());
            shot.setPlayerId(player.getId());
            shot.setQuarter(random.nextInt(4) + 1);
            shot.setGameClock(String.format("%02d:%02d", random.nextInt(12) + 1, random.nextInt(60)));

            BigDecimal x = BigDecimal.valueOf(range[0] + random.nextDouble() * (range[1] - range[0]))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal y = BigDecimal.valueOf(range[2] + random.nextDouble() * (range[3] - range[2]))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal distance = BigDecimal.valueOf(Math.sqrt(x.doubleValue() * x.doubleValue() + y.doubleValue() * y.doubleValue()))
                    .setScale(2, RoundingMode.HALF_UP);

            shot.setX(x);
            shot.setY(y);
            shot.setDistanceFt(distance);

            String zone = ZONE_CN.get(zoneKey);
            String shotType;
            if (zoneKey.contains("3")) {
                shotType = "三分";
            } else if (zoneKey.equals("RIM")) {
                shotType = "两分";
            } else {
                shotType = random.nextDouble() < 0.95 ? "两分" : "罚球";
            }
            shot.setShotType(shotType);
            shot.setZone(zone);
            shot.setMade(random.nextDouble() < range[4] ? 1 : 0);
            shot.setAssistedBy(null);

            batch.add(shot);

            if (batch.size() >= batchSize) {
                shotMapper.batchAdd(batch);
                batch.clear();
                if ((i + 1) % 10_000 == 0) {
                    System.out.println("[ShotDataGenerator] generated " + (i + 1) + " shots");
                }
            }
        }

        if (!batch.isEmpty()) {
            shotMapper.batchAdd(batch);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("[ShotDataGenerator] done: +" + target + " shots, total " + shotMapper.count() + " in " + elapsed + " ms");
    }

    private boolean shouldRun(String[] args) {
        if (args == null) return false;
        for (String arg : args) {
            if ("--generate-shots".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private String pickZone() {
        int total = 0;
        for (int w : ZONE_WEIGHTS) total += w;
        int value = random.nextInt(total);
        int cumulative = 0;
        for (int i = 0; i < ZONE_KEYS.size(); i++) {
            cumulative += ZONE_WEIGHTS[i];
            if (value < cumulative) {
                return ZONE_KEYS.get(i);
            }
        }
        return ZONE_KEYS.get(ZONE_KEYS.size() - 1);
    }
}
