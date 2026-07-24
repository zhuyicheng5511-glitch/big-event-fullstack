package com.example.bigevent.mapper;

import com.example.bigevent.pojo.Category;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CategoryMapper {

    // 新增分类
    @Insert("insert into category(category_name, category_alias, create_user, create_time, update_time) " +
            "values(#{categoryName}, #{categoryAlias}, #{createUser}, now(), now())")
    void add(Category category);

    // 查询当前登录用户创建的所有分类
    @Select("select * from category where create_user = #{userId}")
    List<Category> list(Integer userId);

    // 根据ID查询分类详情
    @Select("select * from category where id = #{id}")
    Category findById(Integer id);

    // 更新分类
    @Update("update category set category_name=#{categoryName}, category_alias=#{categoryAlias}, update_time=now() where id=#{id}")
    void update(Category category);

    // 删除分类
    @Delete("delete from category where id=#{id}")
    void deleteById(Integer id);
}