package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.Season;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SeasonMapper {

    // 新增赛季
    @Insert("insert into season(name, start_date, end_date, is_current, create_time, update_time) " +
            "values(#{name}, #{startDate}, #{endDate}, #{isCurrent}, now(), now())")
    void add(Season season);

    // 条件分页查询（赛季名模糊搜索）
    @Select("<script>" +
            "select * from season " +
            "<where>" +
            "   <if test='name != null and name.length() > 0'> name like concat('%', #{name}, '%') </if> " +
            "</where>" +
            "order by start_date desc" +
            "</script>")
    List<Season> list(@Param("name") String name);

    // 根据ID查询赛季详情
    @Select("select * from season where id = #{id}")
    Season findById(Integer id);

    // 修改赛季
    @Update("update season set name=#{name}, start_date=#{startDate}, end_date=#{endDate}, " +
            "is_current=#{isCurrent}, update_time=now() where id=#{id}")
    int update(Season season);

    // 删除赛季
    @Delete("delete from season where id=#{id}")
    int deleteById(Integer id);
}
