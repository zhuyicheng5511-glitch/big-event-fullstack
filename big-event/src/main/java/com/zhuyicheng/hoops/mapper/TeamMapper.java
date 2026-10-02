package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.Team;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TeamMapper {

    // 新增球队
    @Insert("insert into team(name, name_en, city, conference, division, logo_url, create_time, update_time) " +
            "values(#{name}, #{nameEn}, #{city}, #{conference}, #{division}, #{logoUrl}, now(), now())")
    void add(Team team);

    // 条件分页查询（按球队名或城市模糊搜索）
    @Select("<script>" +
            "select * from team " +
            "<where>" +
            "   <if test='keyword != null and keyword.length() > 0'> " +
            "       (name like concat('%', #{keyword}, '%') or city like concat('%', #{keyword}, '%')) " +
            "   </if> " +
            "</where>" +
            "order by name" +
            "</script>")
    List<Team> list(@Param("keyword") String keyword);

    // 根据ID查询球队详情
    @Select("select * from team where id = #{id}")
    Team findById(Integer id);

    // 修改球队
    @Update("update team set name=#{name}, name_en=#{nameEn}, city=#{city}, conference=#{conference}, " +
            "division=#{division}, logo_url=#{logoUrl}, update_time=now() where id=#{id}")
    int update(Team team);

    // 删除球队
    @Delete("delete from team where id=#{id}")
    int deleteById(Integer id);

    // 批量新增球队
    @Insert("<script>" +
            "insert into team(name, name_en, city, conference, division, logo_url, create_time, update_time) " +
            "values " +
            "<foreach collection='list' item='it' separator=','>" +
            "(#{it.name}, #{it.nameEn}, #{it.city}, #{it.conference}, #{it.division}, #{it.logoUrl}, now(), now())" +
            "</foreach>" +
            "</script>")
    void batchAdd(@Param("list") List<Team> list);
}
