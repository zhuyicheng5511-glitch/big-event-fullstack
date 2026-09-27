package com.zhuyicheng.hoops.mapper;

import com.zhuyicheng.hoops.pojo.Article;
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
    List<Article> list(@Param("userId") Integer userId,
                       @Param("categoryId") Integer categoryId,
                       @Param("state") String state);

    // 3. 修改文章
    //    where 中必须带上 create_user：否则任何登录用户都能改别人的文章（横向越权）
    //    createUser 由 Service 从 ThreadLocal 取出后强制覆盖，绝不信任前端传入的值
    @Update("update article set title=#{title}, content=#{content}, cover_img=#{coverImg}, state=#{state}, category_id=#{categoryId}, update_time=now() " +
            "where id=#{id} and create_user=#{createUser}")
    int update(Article article);

    // 4. 删除文章（同样必须限定 create_user，否则能删掉别人的文章）
    @Delete("delete from article where id=#{id} and create_user=#{userId}")
    int deleteById(@Param("id") Integer id, @Param("userId") Integer userId);
}