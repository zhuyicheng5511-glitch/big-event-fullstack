package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.Category;
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

    // 根据ID查询分类详情（限定 create_user，防止越权查看他人分类）
    @Select("select * from category where id = #{id} and create_user = #{userId}")
    Category findById(@Param("id") Integer id, @Param("userId") Integer userId);

    // 更新分类
    // where 中必须带上 create_user：否则任何登录用户都能改别人的分类（横向越权）
    // createUser 由 Service 从 ThreadLocal 取出后强制覆盖，绝不信任前端传入的值
    @Update("update category set category_name=#{categoryName}, category_alias=#{categoryAlias}, update_time=now() " +
            "where id=#{id} and create_user=#{createUser}")
    int update(Category category);

    // 删除分类（同样必须限定 create_user，否则能删掉别人的分类）
    @Delete("delete from category where id=#{id} and create_user=#{userId}")
    int deleteById(@Param("id") Integer id, @Param("userId") Integer userId);
}