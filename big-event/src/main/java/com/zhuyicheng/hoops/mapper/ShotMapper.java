package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.Shot;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShotMapper {

    @Insert("insert into shot(game_id, player_id, quarter, game_clock, x, y, distance_ft, shot_type, made, zone, assisted_by, create_time) " +
            "values(#{gameId}, #{playerId}, #{quarter}, #{gameClock}, #{x}, #{y}, #{distanceFt}, #{shotType}, #{made}, #{zone}, #{assistedBy}, now())")
    void add(Shot shot);

    @Select("<script>" +
            "select * from shot " +
            "<where>" +
            "   <if test='gameId != null'> game_id = #{gameId} </if>" +
            "   <if test='playerId != null'> and player_id = #{playerId} </if>" +
            "   <if test='zone != null and zone.length() > 0'> and zone = #{zone} </if>" +
            "</where>" +
            "order by create_time desc" +
            "</script>")
    List<Shot> list(@Param("gameId") Integer gameId,
                    @Param("playerId") Integer playerId,
                    @Param("zone") String zone);

    @Select("select * from shot where id = #{id}")
    Shot findById(Long id);

    @Update("update shot set game_id=#{gameId}, player_id=#{playerId}, quarter=#{quarter}, game_clock=#{gameClock}, " +
            "x=#{x}, y=#{y}, distance_ft=#{distanceFt}, shot_type=#{shotType}, made=#{made}, zone=#{zone}, " +
            "assisted_by=#{assistedBy} where id=#{id}")
    int update(Shot shot);

    @Delete("delete from shot where id = #{id}")
    int deleteById(Long id);

    @Select("select count(*) from shot")
    long count();

    @Insert("<script>" +
            "insert into shot(game_id, player_id, quarter, game_clock, x, y, distance_ft, shot_type, made, zone, assisted_by, create_time) " +
            "values " +
            "<foreach collection='list' item='it' separator=','>" +
            "(#{it.gameId}, #{it.playerId}, #{it.quarter}, #{it.gameClock}, #{it.x}, #{it.y}, #{it.distanceFt}, " +
            "#{it.shotType}, #{it.made}, #{it.zone}, #{it.assistedBy}, now())" +
            "</foreach>" +
            "</script>")
    void batchAdd(@Param("list") List<Shot> list);
}
