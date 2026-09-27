package com.example.bigevent.controller;

import com.example.bigevent.pojo.Category;
import com.example.bigevent.pojo.Result;
import com.example.bigevent.service.CategoryService;
import com.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
        // 只允许查看自己创建的分类
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Category category = categoryService.findById(id, userId);
        if (category == null) {
            return Result.error("分类不存在，或您无权访问该分类");
        }
        return Result.success(category);
    }

    // 4. 修改文章分类
    @PutMapping
    public Result update(@RequestBody @Validated Category category) {
        if (categoryService.update(category) == 0) {
            return Result.error("分类不存在，或您无权操作该分类");
        }
        return Result.success();
    }

    // 5. 删除文章分类
    @DeleteMapping
    public Result delete(@RequestParam Integer id) {
        try {
            if (categoryService.deleteById(id) == 0) {
                return Result.error("分类不存在，或您无权操作该分类");
            }
        } catch (DataIntegrityViolationException e) {
            // 该分类下还有文章时，外键 fk_article_category 会阻止删除（MySQL 报 1451），
            // 这里捕获并给出明确原因，而不是让全局处理器统一回「服务器繁忙，请稍候再试」
            return Result.error("该分类下已存在文章，无法删除");
        }
        return Result.success();
    }
}