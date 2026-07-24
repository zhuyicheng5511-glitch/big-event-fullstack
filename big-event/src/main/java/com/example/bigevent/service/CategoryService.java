package com.example.bigevent.service;

import com.example.bigevent.pojo.Category;
import java.util.List;

public interface CategoryService {
    // 新增分类
    void add(Category category);

    // 获取分类列表
    List<Category> list(Integer userId);

    // 获取分类详情
    Category findById(Integer id);

    // 修改分类
    void update(Category category);

    // 删除分类
    void deleteById(Integer id);
}