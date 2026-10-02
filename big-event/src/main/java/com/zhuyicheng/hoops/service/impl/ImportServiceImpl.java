package com.zhuyicheng.hoops.service.impl;

import com.zhuyicheng.hoops.mapper.*;
import com.zhuyicheng.hoops.pojo.*;
import com.zhuyicheng.hoops.service.ImportService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ImportServiceImpl implements ImportService {

    @Autowired
    private TeamMapper teamMapper;
    @Autowired
    private PlayerMapper playerMapper;
    @Autowired
    private SeasonMapper seasonMapper;
    @Autowired
    private GameMapper gameMapper;
    @Autowired
    private PlayerGameStatMapper playerGameStatMapper;
    @Autowired
    private ShotMapper shotMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importCsv(String entity, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            ImportResult result = ImportResult.empty();
            result.getErrors().add(new ImportError(0, "file", "上传文件不能为空"));
            return result;
        }
        switch (entity) {
            case "team": return importTeams(file);
            case "player": return importPlayers(file);
            case "season": return importSeasons(file);
            case "game": return importGames(file);
            case "player-game-stat": return importPlayerGameStats(file);
            case "shot": return importShots(file);
            default:
                ImportResult result = ImportResult.empty();
                result.getErrors().add(new ImportError(0, "entity", "不支持的导入类型: " + entity));
                return result;
        }
    }

    // ========================= team =========================
    private ImportResult importTeams(MultipartFile file) {
        List<ImportError> errors = new ArrayList<>();
        List<Team> list = new ArrayList<>();
        Set<String> existingNames = new HashSet<>();
        teamMapper.list(null).forEach(t -> existingNames.add(t.getName()));
        Set<String> seen = new HashSet<>();

        parse(file, record -> {
            String name = value(record, "name");
            if (missing(name, "name", record, errors)) return;
            if (duplicate(name, seen, "name", record, errors)) return;
            if (existingNames.contains(name)) {
                errors.add(new ImportError(lineNumber(record), "name", "球队名已存在: " + name));
                return;
            }
            Team team = new Team();
            team.setName(name);
            team.setNameEn(value(record, "name_en"));
            team.setCity(value(record, "city"));
            team.setConference(value(record, "conference"));
            team.setDivision(value(record, "division"));
            team.setLogoUrl(value(record, "logo_url"));
            list.add(team);
            seen.add(name);
        });

        return saveBatch(list, errors, teamMapper::batchAdd);
    }

    // ========================= season =========================
    private ImportResult importSeasons(MultipartFile file) {
        List<ImportError> errors = new ArrayList<>();
        List<Season> list = new ArrayList<>();
        Set<String> existingNames = new HashSet<>();
        seasonMapper.list(null).forEach(s -> existingNames.add(s.getName()));
        Set<String> seen = new HashSet<>();

        parse(file, record -> {
            String name = value(record, "name");
            if (missing(name, "name", record, errors)) return;
            if (duplicate(name, seen, "name", record, errors)) return;
            if (existingNames.contains(name)) {
                errors.add(new ImportError(lineNumber(record), "name", "赛季名已存在: " + name));
                return;
            }
            Season season = new Season();
            season.setName(name);
            season.setStartDate(parseDate(value(record, "start_date"), "start_date", record, errors));
            season.setEndDate(parseDate(value(record, "end_date"), "end_date", record, errors));
            season.setIsCurrent(parse01(value(record, "is_current"), "is_current", record, errors));
            list.add(season);
            seen.add(name);
        });

        return saveBatch(list, errors, seasonMapper::batchAdd);
    }

    // ========================= player =========================
    private ImportResult importPlayers(MultipartFile file) {
        List<ImportError> errors = new ArrayList<>();
        List<Player> list = new ArrayList<>();
        Map<String, Integer> teamMap = new HashMap<>();
        teamMapper.list(null).forEach(t -> teamMap.put(t.getName(), t.getId()));
        Set<String> existingNames = new HashSet<>();
        playerMapper.list(null, null).forEach(p -> existingNames.add(p.getName()));
        Set<String> seen = new HashSet<>();

        parse(file, record -> {
            String name = value(record, "name");
            if (missing(name, "name", record, errors)) return;
            if (duplicate(name, seen, "name", record, errors)) return;
            if (existingNames.contains(name)) {
                errors.add(new ImportError(lineNumber(record), "name", "球员名已存在: " + name));
                return;
            }
            String teamName = value(record, "team");
            Integer teamId = resolveFk(teamMap, teamName, "team", record, errors);
            if (teamId == null) return;

            Player player = new Player();
            player.setName(name);
            player.setNameEn(value(record, "name_en"));
            player.setTeamId(teamId);
            player.setJerseyNumber(value(record, "jersey_number"));
            player.setPosition(value(record, "position"));
            player.setHeightCm(parseInt(value(record, "height_cm"), "height_cm", record, errors));
            player.setWeightKg(parseInt(value(record, "weight_kg"), "weight_kg", record, errors));
            player.setBirthDate(parseDate(value(record, "birth_date"), "birth_date", record, errors));
            player.setCountry(value(record, "country"));
            player.setAvatarUrl(value(record, "avatar_url"));
            player.setIsActive(1);
            list.add(player);
            seen.add(name);
        });

        return saveBatch(list, errors, playerMapper::batchAdd);
    }

    // ========================= game =========================
    private ImportResult importGames(MultipartFile file) {
        List<ImportError> errors = new ArrayList<>();
        List<Game> list = new ArrayList<>();
        Map<String, Integer> teamMap = new HashMap<>();
        teamMapper.list(null).forEach(t -> teamMap.put(t.getName(), t.getId()));
        Map<String, Integer> seasonMap = new HashMap<>();
        seasonMapper.list(null).forEach(s -> seasonMap.put(s.getName(), s.getId()));
        // key: date_home_away
        Set<String> existingKeys = new HashSet<>();
        gameMapper.list(null, null, null).forEach(g -> existingKeys.add(gameKey(g)));
        Set<String> seen = new HashSet<>();

        parse(file, record -> {
            String seasonName = value(record, "season");
            String dateStr = value(record, "date");
            String home = value(record, "home_team");
            String away = value(record, "away_team");
            if (missing(seasonName, "season", record, errors)) return;
            if (missing(dateStr, "date", record, errors)) return;
            if (missing(home, "home_team", record, errors)) return;
            if (missing(away, "away_team", record, errors)) return;

            Integer seasonId = resolveFk(seasonMap, seasonName, "season", record, errors);
            Integer homeId = resolveFk(teamMap, home, "home_team", record, errors);
            Integer awayId = resolveFk(teamMap, away, "away_team", record, errors);
            if (seasonId == null || homeId == null || awayId == null) return;

            LocalDateTime gameDate = parseDateTime(dateStr, "date", record, errors);
            if (gameDate == null) return;
            String key = dateStr + "_" + home + "_" + away;
            if (duplicate(key, seen, "home_team/away_team/date", record, errors)) return;
            if (existingKeys.contains(key)) {
                errors.add(new ImportError(lineNumber(record), "home_team/away_team/date", "比赛已存在: " + key));
                return;
            }

            Game game = new Game();
            game.setSeasonId(seasonId);
            game.setHomeTeamId(homeId);
            game.setAwayTeamId(awayId);
            game.setGameDate(gameDate);
            game.setHomeScore(parseInt(value(record, "home_score"), "home_score", record, errors));
            game.setAwayScore(parseInt(value(record, "away_score"), "away_score", record, errors));
            game.setStatus(value(record, "status"));
            game.setVenue(value(record, "venue"));
            list.add(game);
            seen.add(key);
        });

        return saveBatch(list, errors, gameMapper::batchAdd);
    }

    // ========================= player-game-stat =========================
    private ImportResult importPlayerGameStats(MultipartFile file) {
        List<ImportError> errors = new ArrayList<>();
        List<PlayerGameStat> list = new ArrayList<>();
        Map<String, Integer> gameMap = loadGameKeyMap();
        Map<String, Integer> playerMap = new HashMap<>();
        playerMapper.list(null, null).forEach(p -> playerMap.put(p.getName(), p.getId()));
        Map<String, Integer> teamMap = new HashMap<>();
        teamMapper.list(null).forEach(t -> teamMap.put(t.getName(), t.getId()));
        // key: gameId_playerId
        Set<String> existingKeys = new HashSet<>();
        playerGameStatMapper.list(null, null, null).forEach(s -> existingKeys.add(s.getGameId() + "_" + s.getPlayerId()));
        Set<String> seen = new HashSet<>();

        parse(file, record -> {
            String gameKey = value(record, "game_key");
            String playerName = value(record, "player");
            String teamName = value(record, "team");
            if (missing(gameKey, "game_key", record, errors)) return;
            if (missing(playerName, "player", record, errors)) return;
            if (missing(teamName, "team", record, errors)) return;

            Integer gameId = resolveFk(gameMap, gameKey, "game_key", record, errors);
            Integer playerId = resolveFk(playerMap, playerName, "player", record, errors);
            Integer teamId = resolveFk(teamMap, teamName, "team", record, errors);
            if (gameId == null || playerId == null || teamId == null) return;

            String key = gameId + "_" + playerId;
            if (duplicate(key, seen, "game_key/player", record, errors)) return;
            if (existingKeys.contains(key)) {
                errors.add(new ImportError(lineNumber(record), "game_key/player", "该球员本场比赛数据已存在: " + key));
                return;
            }

            PlayerGameStat stat = new PlayerGameStat();
            stat.setGameId(gameId);
            stat.setPlayerId(playerId);
            stat.setTeamId(teamId);
            stat.setIsStarter(parse01(value(record, "is_starter"), "is_starter", record, errors));
            stat.setMinutes(parseDecimal(value(record, "minutes"), "minutes", record, errors));
            stat.setPoints(parseInt(value(record, "points"), "points", record, errors));
            stat.setRebounds(parseInt(value(record, "rebounds"), "rebounds", record, errors));
            stat.setAssists(parseInt(value(record, "assists"), "assists", record, errors));
            stat.setSteals(parseInt(value(record, "steals"), "steals", record, errors));
            stat.setBlocks(parseInt(value(record, "blocks"), "blocks", record, errors));
            stat.setTurnovers(parseInt(value(record, "turnovers"), "turnovers", record, errors));
            stat.setFouls(parseInt(value(record, "fouls"), "fouls", record, errors));
            stat.setFgm(parseInt(value(record, "fgm"), "fgm", record, errors));
            stat.setFga(parseInt(value(record, "fga"), "fga", record, errors));
            stat.setFg3m(parseInt(value(record, "fg3m"), "fg3m", record, errors));
            stat.setFg3a(parseInt(value(record, "fg3a"), "fg3a", record, errors));
            stat.setFtm(parseInt(value(record, "ftm"), "ftm", record, errors));
            stat.setFta(parseInt(value(record, "fta"), "fta", record, errors));
            stat.setPlusMinus(parseInt(value(record, "plus_minus"), "plus_minus", record, errors));
            list.add(stat);
            seen.add(key);
        });

        return saveBatch(list, errors, playerGameStatMapper::batchAdd);
    }

    // ========================= shot =========================
    private ImportResult importShots(MultipartFile file) {
        List<ImportError> errors = new ArrayList<>();
        List<Shot> list = new ArrayList<>();
        Map<String, Integer> gameMap = loadGameKeyMap();
        Map<String, Integer> playerMap = new HashMap<>();
        playerMapper.list(null, null).forEach(p -> playerMap.put(p.getName(), p.getId()));

        parse(file, record -> {
            String gameKey = value(record, "game_key");
            String playerName = value(record, "player");
            if (missing(gameKey, "game_key", record, errors)) return;
            if (missing(playerName, "player", record, errors)) return;

            Integer gameId = resolveFk(gameMap, gameKey, "game_key", record, errors);
            Integer playerId = resolveFk(playerMap, playerName, "player", record, errors);
            if (gameId == null || playerId == null) return;

            Shot shot = new Shot();
            shot.setGameId(gameId);
            shot.setPlayerId(playerId);
            shot.setQuarter(parseInt(value(record, "quarter"), "quarter", record, errors));
            shot.setGameClock(value(record, "game_clock"));
            shot.setX(parseDecimal(value(record, "x"), "x", record, errors));
            shot.setY(parseDecimal(value(record, "y"), "y", record, errors));
            shot.setDistanceFt(parseDecimal(value(record, "distance_ft"), "distance_ft", record, errors));
            shot.setShotType(value(record, "shot_type"));
            shot.setMade(parse01(value(record, "made"), "made", record, errors));
            shot.setZone(value(record, "zone"));
            list.add(shot);
        });

        return saveBatch(list, errors, shotMapper::batchAdd);
    }

    // ========================= helpers =========================

    private interface BatchConsumer<T> {
        void accept(List<T> list);
    }

    private <T> ImportResult saveBatch(List<T> list, List<ImportError> errors, BatchConsumer<T> inserter) {
        if (!errors.isEmpty()) {
            return buildResult(errors.size(), 0, errors);
        }
        inserter.accept(list);
        return buildResult(list.size(), list.size(), errors);
    }

    private ImportResult buildResult(int total, int success, List<ImportError> errors) {
        ImportResult result = new ImportResult();
        result.setTotalRows(total);
        result.setSuccessRows(success);
        result.setFailureRows(errors.size());
        result.setErrors(errors);
        return result;
    }

    private Map<String, Integer> loadGameKeyMap() {
        Map<String, Integer> map = new HashMap<>();
        Map<Integer, String> teamNames = new HashMap<>();
        teamMapper.list(null).forEach(t -> teamNames.put(t.getId(), t.getName()));
        gameMapper.list(null, null, null).forEach(g -> {
            String home = teamNames.get(g.getHomeTeamId());
            String away = teamNames.get(g.getAwayTeamId());
            if (home != null && away != null) {
                String key = g.getGameDate().toLocalDate().format(DATE_FORMATTER) + "_" + home + "_" + away;
                map.put(key, g.getId());
            }
        });
        return map;
    }

    private String gameKey(Game g) {
        return g.getGameDate().toLocalDate().format(DATE_FORMATTER) + "_" + g.getHomeTeamId() + "_" + g.getAwayTeamId();
    }

    @FunctionalInterface
    private interface RecordConsumer {
        void accept(CSVRecord record);
    }

    private void parse(MultipartFile file, RecordConsumer consumer) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();
        try (InputStream raw = file.getInputStream();
             InputStream in = skipBom(raw);
             CSVParser parser = new CSVParser(new InputStreamReader(in, StandardCharsets.UTF_8), format)) {
            for (CSVRecord record : parser) {
                consumer.accept(record);
            }
        } catch (IOException e) {
            throw new RuntimeException("CSV 解析失败", e);
        }
    }

    /**
     * 跳过 UTF-8 BOM（EF BB BF）。
     * Excel 与 Windows 记事本导出的 CSV 默认带 BOM，不处理会导致表头第一个列名
     * 变成 "﻿name"，进而让所有行的第一列都被误判为「字段不能为空」。
     */
    private InputStream skipBom(InputStream in) throws IOException {
        PushbackInputStream pushback = new PushbackInputStream(in, 3);
        byte[] bom = new byte[3];
        int read = pushback.read(bom, 0, 3);
        boolean isBom = read == 3
                && (bom[0] & 0xFF) == 0xEF
                && (bom[1] & 0xFF) == 0xBB
                && (bom[2] & 0xFF) == 0xBF;
        if (!isBom && read > 0) {
            pushback.unread(bom, 0, read);   // 不是 BOM 就还回去，避免吃掉正常内容
        }
        return pushback;
    }

    /** CSV 文件中的物理行号：表头为第 1 行，首条数据为第 2 行。
     *  commons-csv 在 skipHeaderRecord 模式下数据行从 1 开始计数，故统一 +1。 */
    private int lineNumber(CSVRecord record) {
        return (int) record.getRecordNumber() + 1;
    }

    private String value(CSVRecord record, String header) {
        return record.isMapped(header) ? record.get(header) : null;
    }

    private boolean missing(String value, String field, CSVRecord record, List<ImportError> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new ImportError(lineNumber(record), field, "字段不能为空"));
            return true;
        }
        return false;
    }

    private boolean duplicate(String value, Set<String> seen, String field, CSVRecord record, List<ImportError> errors) {
        if (seen.contains(value)) {
            errors.add(new ImportError(lineNumber(record), field, "文件内重复: " + value));
            return true;
        }
        return false;
    }

    private Integer resolveFk(Map<String, Integer> map, String value, String field, CSVRecord record, List<ImportError> errors) {
        if (value == null || value.isBlank()) return null;
        Integer id = map.get(value.trim());
        if (id == null) {
            errors.add(new ImportError(lineNumber(record), field, "不存在: " + value));
        }
        return id;
    }

    private Integer parseInt(String value, String field, CSVRecord record, List<ImportError> errors) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            errors.add(new ImportError(lineNumber(record), field, "必须是整数: " + value));
            return null;
        }
    }

    private Integer parse01(String value, String field, CSVRecord record, List<ImportError> errors) {
        Integer v = parseInt(value, field, record, errors);
        if (v == null) return null;
        if (v != 0 && v != 1) {
            errors.add(new ImportError(lineNumber(record), field, "只能是 0 或 1: " + value));
            return null;
        }
        return v;
    }

    private BigDecimal parseDecimal(String value, String field, CSVRecord record, List<ImportError> errors) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            errors.add(new ImportError(lineNumber(record), field, "必须是数字: " + value));
            return null;
        }
    }

    private LocalDate parseDate(String value, String field, CSVRecord record, List<ImportError> errors) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            errors.add(new ImportError(lineNumber(record), field, "日期格式必须为 yyyy-MM-dd: " + value));
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value, String field, CSVRecord record, List<ImportError> errors) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value.trim(), DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            errors.add(new ImportError(lineNumber(record), field, "时间格式必须为 yyyy-MM-dd HH:mm:ss: " + value));
            return null;
        }
    }
}
