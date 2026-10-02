package com.zhuyicheng.hoops.runner;

import com.zhuyicheng.hoops.mapper.*;
import com.zhuyicheng.hoops.pojo.Game;
import com.zhuyicheng.hoops.pojo.Player;
import com.zhuyicheng.hoops.pojo.PlayerGameStat;
import com.zhuyicheng.hoops.pojo.Season;
import com.zhuyicheng.hoops.pojo.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Component
public class PlayerGameStatDataGeneratorRunner implements CommandLineRunner {

    @Autowired
    private GameMapper gameMapper;
    @Autowired
    private PlayerMapper playerMapper;
    @Autowired
    private TeamMapper teamMapper;
    @Autowired
    private SeasonMapper seasonMapper;
    @Autowired
    private PlayerGameStatMapper playerGameStatMapper;

    private final Random random = new Random(42);

    @Override
    public void run(String... args) throws Exception {
        if (!shouldRun(args)) {
            return;
        }

        List<Season> seasons = seasonMapper.list(null);
        List<Team> teams = teamMapper.list(null);
        List<Player> players = playerMapper.list(null, null);

        if (seasons.isEmpty() || teams.size() < 2 || players.isEmpty()) {
            System.out.println("[PlayerGameStatGenerator] missing season/team/player data");
            return;
        }

        Season season = seasons.get(0);
        int seasonId = season.getId();

        // 确保有足够多的比赛：目标 1200 场
        List<Game> existingGames = gameMapper.list(seasonId, null, null);
        int targetGames = 1200;
        List<Game> newGames = new ArrayList<>();

        if (existingGames.size() < targetGames) {
            for (int i = existingGames.size(); i < targetGames; i++) {
                Game g = new Game();
                g.setSeasonId(seasonId);
                Team home = teams.get(random.nextInt(teams.size()));
                Team away = teams.get(random.nextInt(teams.size()));
                while (away.getId().equals(home.getId())) {
                    away = teams.get(random.nextInt(teams.size()));
                }
                g.setHomeTeamId(home.getId());
                g.setAwayTeamId(away.getId());
                g.setGameDate(LocalDateTime.of(2025, 10, 1, 0, 0, 0).plusDays(i));
                g.setHomeScore(100 + random.nextInt(50));
                g.setAwayScore(100 + random.nextInt(50));
                g.setStatus("已结束");
                g.setVenue("Generated");
                newGames.add(g);
            }
            batchSaveGames(newGames);
        }

        // 合并已有比赛和新比赛，用于后续补全球员数据
        List<Game> allGames = new ArrayList<>(existingGames);
        allGames.addAll(newGames);

        // 加载已存在的 (gameId, playerId) 对，避免唯一约束冲突
        Set<String> existingPairs = new HashSet<>();
        for (PlayerGameStat stat : playerGameStatMapper.listAllKeyPairs()) {
            existingPairs.add(stat.getGameId() + ":" + stat.getPlayerId());
        }

        // 每场比赛生成 25 条球员数据；已有数据的不再重复插入
        int statsPerGame = 25;
        int batchSize = 1000;
        long startTime = System.currentTimeMillis();
        List<PlayerGameStat> batch = new ArrayList<>(batchSize);

        int generated = 0;
        int skippedGames = 0;
        for (Game game : allGames) {
            int countForGame = 0;
            Collections.shuffle(players, random);
            for (Player player : players) {
                if (countForGame >= statsPerGame) {
                    break;
                }
                String key = game.getId() + ":" + player.getId();
                if (existingPairs.contains(key)) {
                    continue;
                }

                PlayerGameStat stat = buildStat(game, player);
                batch.add(stat);
                existingPairs.add(key);
                countForGame++;
                generated++;

                if (batch.size() >= batchSize) {
                    playerGameStatMapper.batchAdd(batch);
                    batch.clear();
                    if (generated % 5000 == 0) {
                        System.out.println("[PlayerGameStatGenerator] generated " + generated + " stats");
                    }
                }
            }
            if (countForGame == 0) {
                skippedGames++;
            }
        }

        if (!batch.isEmpty()) {
            playerGameStatMapper.batchAdd(batch);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("[PlayerGameStatGenerator] done: " + generated + " stats in " + elapsed + " ms, games=" + allGames.size() + ", skipped=" + skippedGames);
    }

    private PlayerGameStat buildStat(Game game, Player player) {
        PlayerGameStat stat = new PlayerGameStat();
        stat.setGameId(game.getId());
        stat.setPlayerId(player.getId());
        stat.setTeamId(player.getTeamId());
        stat.setIsStarter(random.nextInt(2));
        stat.setMinutes(BigDecimal.valueOf(10 + random.nextInt(30) + random.nextDouble()).setScale(1, java.math.RoundingMode.HALF_UP));
        stat.setPoints(2 + random.nextInt(25));
        stat.setRebounds(random.nextInt(12));
        stat.setAssists(random.nextInt(10));
        stat.setSteals(random.nextInt(5));
        stat.setBlocks(random.nextInt(5));
        stat.setTurnovers(random.nextInt(6));
        stat.setFouls(random.nextInt(6));
        stat.setFgm(random.nextInt(12));
        stat.setFga(stat.getFgm() + random.nextInt(8));
        stat.setFg3m(random.nextInt(5));
        stat.setFg3a(stat.getFg3m() + random.nextInt(5));
        stat.setFtm(random.nextInt(6));
        stat.setFta(stat.getFtm() + random.nextInt(4));
        stat.setPlusMinus(random.nextInt(20) - 10);
        return stat;
    }

    private void batchSaveGames(List<Game> games) {
        // 使用 gameMapper 没有批量插入方法，逐条插入
        for (Game game : games) {
            gameMapper.add(game);
        }
    }

    private boolean shouldRun(String[] args) {
        if (args == null) return false;
        for (String arg : args) {
            if ("--generate-stats".equals(arg)) {
                return true;
            }
        }
        return false;
    }
}
