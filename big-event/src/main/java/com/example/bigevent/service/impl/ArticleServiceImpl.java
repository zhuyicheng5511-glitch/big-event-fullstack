package com.example.bigevent.service.impl;

import com.example.bigevent.mapper.ArticleMapper;
import com.example.bigevent.pojo.Article;
import com.example.bigevent.pojo.PageBean;
import com.example.bigevent.service.ArticleService;
import com.example.bigevent.utils.ThreadLocalUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public void add(Article article) {
        articleMapper.add(article);
    }

    @Override
    public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
        // 1. 创建响应承载对象
        PageBean<Article> pb = new PageBean<>();

        // 2. 开启分页查询 (PageHelper 核心机制：自动通过拦截器在接下来的第一条 SQL 后面追加 LIMIT 语句)
        PageHelper.startPage(pageNum, pageSize);

        // 3. 调用 Mapper 查询 (获取当前登录用户的数据)
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        List<Article> list = articleMapper.list(userId, categoryId, state);

        // 4. 将查询返回的 List 强转为 PageHelper 的 Page 类型（里面封装了 total 和当前页结果）
        Page<Article> p = (Page<Article>) list;

        // 5. 将数据填充进 PageBean 返回
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());

        return pb;
    }

    @Override
    public int update(Article article) {
        // 强制用当前登录用户的 id 覆盖前端传来的 createUser，
        // 否则攻击者可以在请求体里伪造 createUser 绕过归属校验
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        article.setCreateUser(userId);

        return articleMapper.update(article);
    }

    @Override
    public int deleteById(Integer id) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return articleMapper.deleteById(id, userId);
    }
}