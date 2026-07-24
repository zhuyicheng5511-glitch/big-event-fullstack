package com.example.bigevent.mapper;

import com.example.bigevent.pojo.Article;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArticleMapper {

    // 1. 新增文章
    @Insert("insert into article(title, content, cover_img, state, category_id, create_user, create_time, update_time) " +
            "values(#{title}, #{content}, #{coverImg}, #{state}, #{categoryId}, #{createUser}, now(), now())")
    void add(Article article);

    // 2. 条件分页查询（利用 <script> 标签编写动态 SQL，实现有条件就拼接，没条件就查全部）
    @Select("<script>" +
            "select * from article " +
            "<where>" +
            "   create_user = #{userId} " +
            "   <if test='categoryId != null'> and category_id = #{categoryId} </if> " +
            "   <if test='state != null'> and state = #{state} </if> " +
            "</where>" +
            "</script>")
    List<Article> list(Integer userId, Integer categoryId, String state);

    @Update("update article set title=#{title}, content=#{content}, cover_img=#{coverImg}, state=#{state}, category_id=#{categoryId}, update_time=now() where id=#{id}")
    void update(Article article);

    @Delete("delete from article where id=#{id}")
    void deleteById(Integer id);
}