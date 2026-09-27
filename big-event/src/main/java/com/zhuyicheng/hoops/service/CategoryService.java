package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.Category;
import java.util.List;

public interface CategoryService {
    // 新增分类
    void add(Category category);

    // 获取分类列表
    List<Category> list(Integer userId);

    // 获取分类详情（需要传入当前登录用户 id 做归属校验）
    Category findById(Integer id, Integer userId);

    // 修改分类，返回受影响行数（0 = 分类不存在，或不属于当前登录用户）
    int update(Category category);

    // 删除分类，返回受影响行数（0 = 分类不存在，或不属于当前登录用户）
    int deleteById(Integer id);
}