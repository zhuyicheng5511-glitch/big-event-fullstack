package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.PlayerGameStat;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PlayerGameStatMapper {

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
