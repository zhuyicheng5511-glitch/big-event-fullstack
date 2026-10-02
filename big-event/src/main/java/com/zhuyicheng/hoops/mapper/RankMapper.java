package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.RankItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RankMapper {

    @Select("<script>" +
            "SELECT " +
            "  p.id AS playerId, " +
            "  p.name AS playerName, " +
            "  t.name AS teamName, " +
            "  p.position, " +
            "  SUM(s.points) AS totalPoints, " +
            "  COUNT(*) AS gamesPlayed, " +
            "  ROUND(AVG(s.points), 1) AS avgPoints, " +
            "  RANK() OVER (ORDER BY SUM(s.points) DESC) AS `rank` " +
            "FROM player_game_stat s " +
            "JOIN player p ON p.id = s.player_id " +
            "JOIN team t ON t.id = p.team_id " +
            "JOIN game g ON g.id = s.game_id " +
            "WHERE g.season_id = #{seasonId} " +
            "<if test=\"teamId != null\"> AND p.team_id = #{teamId} </if>" +
            "<if test=\"position != null and position != ''\"> AND p.position = #{position} </if>" +
            "GROUP BY p.id, p.name, t.name, p.position " +
            "ORDER BY totalPoints DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<RankItem> pointsRank(@Param("seasonId") Integer seasonId,
                              @Param("teamId") Integer teamId,
                              @Param("position") String position,
                              @Param("limit") Integer limit);
}
