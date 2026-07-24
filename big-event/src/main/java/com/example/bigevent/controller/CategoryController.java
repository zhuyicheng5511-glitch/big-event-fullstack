package com.example.bigevent.controller;

import com.example.bigevent.pojo.Category;
import com.example.bigevent.pojo.Result;
import com.example.bigevent.service.CategoryService;
import com.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/category")
@Validated
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // 1. 新增文章分类
    @PostMapping
    public Result add(@RequestBody @Validated Category category) {
        // 从 ThreadLocal 中获取当前登录用户的 ID，并绑定为创建人
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        category.setCreateUser(userId);

        categoryService.add(category);
        return Result.success();
    }

    // 2. 获取当前用户创建的所有文章分类
    @GetMapping
    public Result<List<Category>> list() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        List<Category> list = categoryService.list(userId);
        return Result.success(list);
    }

    // 3. 获取分类详情（供修改时回显数据使用）
    @GetMapping("/detail")
    public Result<Category> detail(@RequestParam Integer id) {
        Category category = categoryService.findById(id);
        return Result.success(category);
    }

    // 4. 修改文章分类
    @PutMapping
    public Result update(@RequestBody @Validated Category category) {
        categoryService.update(category);
        return Result.success();
    }

    // 5. 删除文章分类
    @DeleteMapping
    public Result delete(@RequestParam Integer id) {
        categoryService.deleteById(id);
        return Result.success();
    }
}