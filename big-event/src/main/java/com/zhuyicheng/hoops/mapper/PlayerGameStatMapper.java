package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.PlayerGameStat;
import com.zhuyicheng.hoops.pojo.PlayerStatSummary;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

@Mapper
public interface PlayerGameStatMapper {

    @Select("select game_id as gameId, player_id as playerId from player_game_stat")
    List<PlayerGameStat> listAllKeyPairs();

    @Insert("insert into player_game_stat(game_id, player_id, team_id, is_starter, minutes, points, rebounds, " +
            "assists, steals, blocks, turnovers, fouls, fgm, fga, fg3m, fg3a, ftm, fta, plus_minus, create_time, update_time) " +
            "values(#{gameId}, #{playerId}, #{teamId}, #{isStarter}, #{minutes}, #{points}, #{rebounds}, " +
            "#{assists}, #{steals}, #{blocks}, #{turnovers}, #{fouls}, #{fgm}, #{fga}, #{fg3m}, #{fg3a}, #{ftm}, #{fta}, #{plusMinus}, now(), now())")
    void add(PlayerGameStat stat);

    @Select("<script>" +
            "select * from player_game_stat " +
            "<where>" +
            "   <if test='gameId != null'> game_id = #{gameId} </if>" +
            "   <if test='playerId != null'> and player_id = #{playerId} </if>" +
            "   <if test='teamId != null'> and team_id = #{teamId} </if>" +
            "</where>" +
            "order by game_id desc, points desc" +
            "</script>")
    List<PlayerGameStat> list(@Param("gameId") Integer gameId,
                              @Param("playerId") Integer playerId,
                              @Param("teamId") Integer teamId);

    /**
     * 球员汇总统计：场均数据与命中率直接在数据库里聚合。
     * 明细列表接口受分页上限（单页最多 100 条）限制，拿它当全量来平均会算错，
     * 所以场均类指标一律走这条 SQL —— 无论多少场比赛都只返回一行。
     * seasonId 传 null 表示不限赛季。
     */
    @Select("<script>" +
            "select s.player_id as playerId, p.name as playerName, count(*) as games, " +
            "  round(avg(s.minutes), 1) as avgMinutes, " +
            "  round(avg(s.points), 1) as avgPoints, " +
            "  round(avg(s.rebounds), 1) as avgRebounds, " +
            "  round(avg(s.assists), 1) as avgAssists, " +
            "  round(avg(s.steals), 1) as avgSteals, " +
            "  round(avg(s.blocks), 1) as avgBlocks, " +
            "  round(avg(s.turnovers), 1) as avgTurnovers, " +
            "  sum(s.points) as totalPoints, " +
            "  round(sum(s.fgm) * 100.0 / nullif(sum(s.fga), 0), 1) as fgPct, " +
            "  round(sum(s.fg3m) * 100.0 / nullif(sum(s.fg3a), 0), 1) as fg3Pct, " +
            "  round(sum(s.ftm) * 100.0 / nullif(sum(s.fta), 0), 1) as ftPct " +
            "from player_game_stat s " +
            "join player p on p.id = s.player_id " +
            "join game g on g.id = s.game_id " +
            "where s.player_id = #{playerId} " +
            "<if test=\"seasonId != null\"> and g.season_id = #{seasonId} </if>" +
            "group by s.player_id, p.name" +
            "</script>")
    PlayerStatSummary summary(@Param("playerId") Integer playerId,
                              @Param("seasonId") Integer seasonId);

    @Select("select * from player_game_stat where id = #{id}")
    PlayerGameStat findById(Long id);

    @Update("update player_game_stat set game_id=#{gameId}, player_id=#{playerId}, team_id=#{teamId}, " +
            "is_starter=#{isStarter}, minutes=#{minutes}, points=#{points}, rebounds=#{rebounds}, " +
            "assists=#{assists}, steals=#{steals}, blocks=#{blocks}, turnovers=#{turnovers}, fouls=#{fouls}, " +
            "fgm=#{fgm}, fga=#{fga}, fg3m=#{fg3m}, fg3a=#{fg3a}, ftm=#{ftm}, fta=#{fta}, plus_minus=#{plusMinus}, " +
            "update_time=now() where id=#{id}")
    int update(PlayerGameStat stat);

    @Delete("delete from player_game_stat where id = #{id}")
    int deleteById(Long id);

    @Insert("<script>" +
            "insert into player_game_stat(game_id, player_id, team_id, is_starter, minutes, points, rebounds, " +
            "assists, steals, blocks, turnovers, fouls, fgm, fga, fg3m, fg3a, ftm, fta, plus_minus, create_time, update_time) " +
            "values " +
            "<foreach collection='list' item='it' separator=','>" +
            "(#{it.gameId}, #{it.playerId}, #{it.teamId}, #{it.isStarter}, #{it.minutes}, #{it.points}, #{it.rebounds}, " +
            "#{it.assists}, #{it.steals}, #{it.blocks}, #{it.turnovers}, #{it.fouls}, #{it.fgm}, #{it.fga}, " +
            "#{it.fg3m}, #{it.fg3a}, #{it.ftm}, #{it.fta}, #{it.plusMinus}, now(), now())" +
            "</foreach>" +
            "</script>")
    void batchAdd(@Param("list") List<PlayerGameStat> list);
}
