package com.zhuyicheng.hoops.controller;

import com.zhuyicheng.hoops.pojo.Article;
import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Result;
import com.zhuyicheng.hoops.service.ArticleService;
import com.zhuyicheng.hoops.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/article")
@Validated
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    // 1. 新增文章
    @PostMapping
    public Result add(@RequestBody @Validated Article article) {
        // 绑定当前创建人 ID
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        article.setCreateUser(userId);

        articleService.add(article);
        return Result.success();
    }

    // 2. 获取文章列表（含分页和条件筛选）
    @GetMapping
    public Result<PageBean<Article>> list(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) Integer categoryId, // 非必填筛选参数
            @RequestParam(required = false) String state        // 非必填筛选参数
    ) {
        PageBean<Article> pb = articleService.list(pageNum, pageSize, categoryId, state);
        return Result.success(pb);
    }

    // 3. 修改文章
    @PutMapping
    public Result update(@RequestBody @Validated Article article) {
        // 受影响行数为 0，说明该文章不存在，或不属于当前登录用户
        if (articleService.update(article) == 0) {
            return Result.error("文章不存在，或您无权操作该文章");
        }
        return Result.success();
    }

    // 4. 删除文章
    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable Integer id) {
        if (articleService.deleteById(id) == 0) {
            return Result.error("文章不存在，或您无权操作该文章");
        }
        return Result.success();
    }
}