package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.Article;
import com.zhuyicheng.hoops.pojo.PageBean;

public interface ArticleService {
    // 新增文章
    void add(Article article);

    // 条件分页查询
    PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state);

    // 修改文章，返回受影响行数（0 = 文章不存在，或不属于当前登录用户）
    int update(Article article);

    // 删除文章，返回受影响行数（0 = 文章不存在，或不属于当前登录用户）
    int deleteById(Integer id);
}