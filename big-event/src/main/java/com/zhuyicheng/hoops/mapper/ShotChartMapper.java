package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.ZoneStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShotChartMapper {

    @Select("<script>" +
            "SELECT " +
            "  zone, " +
            "  COUNT(*) AS attempts, " +
            "  SUM(made) AS made, " +
            "  ROUND(SUM(made) / COUNT(*) * 100, 1) AS fgPct " +
            "FROM shot " +
            "WHERE player_id = #{playerId} " +
            "  AND game_id IN (SELECT id FROM game WHERE season_id = #{seasonId}) " +
            "<if test=\"zone != null and zone != ''\"> AND zone = #{zone} </if>" +
            "GROUP BY zone " +
            "</script>")
    List<ZoneStat> chartByPlayerAndSeason(@Param("playerId") Integer playerId,
                                          @Param("seasonId") Integer seasonId,
                                          @Param("zone") String zone);
}
