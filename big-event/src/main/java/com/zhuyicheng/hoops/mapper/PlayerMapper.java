package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.Player;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PlayerMapper {

    // 新增球员
    @Insert("insert into player(name, name_en, team_id, jersey_number, position, height_cm, weight_kg, " +
            "birth_date, country, avatar_url, is_active, create_time, update_time) " +
            "values(#{name}, #{nameEn}, #{teamId}, #{jerseyNumber}, #{position}, #{heightCm}, #{weightKg}, " +
            "#{birthDate}, #{country}, #{avatarUrl}, #{isActive}, now(), now())")
    void add(Player player);

    // 条件分页查询（按球员名模糊搜索 + 按球队筛选）
    @Select("<script>" +
            "select * from player " +
            "<where>" +
            "   <if test='name != null and name.length() > 0'> name like concat('%', #{name}, '%') </if> " +
            "   <if test='teamId != null'> and team_id = #{teamId} </if> " +
            "</where>" +
            "order by name" +
            "</script>")
    List<Player> list(@Param("name") String name, @Param("teamId") Integer teamId);

    // 根据ID查询球员详情
    @Select("select * from player where id = #{id}")
    Player findById(Integer id);

    // 修改球员
    @Update("update player set name=#{name}, name_en=#{nameEn}, team_id=#{teamId}, jersey_number=#{jerseyNumber}, " +
            "position=#{position}, height_cm=#{heightCm}, weight_kg=#{weightKg}, birth_date=#{birthDate}, " +
            "country=#{country}, avatar_url=#{avatarUrl}, is_active=#{isActive}, update_time=now() where id=#{id}")
    int update(Player player);

    // 删除球员
    @Delete("delete from player where id=#{id}")
    int deleteById(Integer id);
}
