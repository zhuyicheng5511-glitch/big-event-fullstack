package com.example.bigevent.service.impl;

import com.example.bigevent.mapper.CategoryMapper;
import com.example.bigevent.pojo.Category;
import com.example.bigevent.service.CategoryService;
import com.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public void add(Category category) {
        categoryMapper.add(category);
    }

    @Override
    public List<Category> list(Integer userId) {
        return categoryMapper.list(userId);
    }

    @Override
    public Category findById(Integer id, Integer userId) {
        return categoryMapper.findById(id, userId);
    }

    @Override
    public int update(Category category) {
        // 强制用当前登录用户的 id 覆盖前端传来的 createUser，
        // 否则攻击者可以在请求体里伪造 createUser 绕过归属校验
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        category.setCreateUser(userId);

        return categoryMapper.update(category);
    }

    @Override
    public int deleteById(Integer id) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return categoryMapper.deleteById(id, userId);
    }
}