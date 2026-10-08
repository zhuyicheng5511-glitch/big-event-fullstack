package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.Game;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GameMapper {

    // 新增比赛
    // useGeneratedKeys：回填自增主键 —— PlayerGameStatDataGeneratorRunner 批量造完比赛后
    // 要立刻用 game.getId() 挂单场数据，不回填就是「Column 'game_id' cannot be null」。
    @Insert("insert into game(season_id, home_team_id, away_team_id, game_date, home_score, away_score, status, venue, create_time, update_time) " +
            "values(#{seasonId}, #{homeTeamId}, #{awayTeamId}, #{gameDate}, #{homeScore}, #{awayScore}, #{status}, #{venue}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Game game);

    // 条件分页查询（按赛季、球队、状态筛选）
    @Select("<script>" +
            "select * from game " +
            "<where>" +
            "   <if test='seasonId != null'> season_id = #{seasonId} </if> " +
            "   <if test='teamId != null'> and (home_team_id = #{teamId} or away_team_id = #{teamId}) </if> " +
            "   <if test='status != null and status.length() > 0'> and status = #{status} </if> " +
            "</where>" +
            "order by game_date desc" +
            "</script>")
    List<Game> list(@Param("seasonId") Integer seasonId,
                    @Param("teamId") Integer teamId,
                    @Param("status") String status);

    // 根据ID查询比赛详情
    @Select("select * from game where id = #{id}")
    Game findById(Integer id);

    // 修改比赛
    @Update("update game set season_id=#{seasonId}, home_team_id=#{homeTeamId}, away_team_id=#{awayTeamId}, " +
            "game_date=#{gameDate}, home_score=#{homeScore}, away_score=#{awayScore}, status=#{status}, " +
            "venue=#{venue}, update_time=now() where id=#{id}")
    int update(Game game);

    // 删除比赛
    @Delete("delete from game where id=#{id}")
    int deleteById(Integer id);

    // 批量新增比赛
    @Insert("<script>" +
            "insert into game(season_id, home_team_id, away_team_id, game_date, home_score, away_score, status, venue, create_time, update_time) " +
            "values " +
            "<foreach collection='list' item='it' separator=','>" +
            "(#{it.seasonId}, #{it.homeTeamId}, #{it.awayTeamId}, #{it.gameDate}, #{it.homeScore}, #{it.awayScore}, #{it.status}, #{it.venue}, now(), now())" +
            "</foreach>" +
            "</script>")
    void batchAdd(@Param("list") List<Game> list);
}
