package com.zhuyicheng.hoops.controller;

import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Result;
import com.zhuyicheng.hoops.pojo.Season;
import com.zhuyicheng.hoops.service.SeasonService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/season")
@Validated
public class SeasonController {

    @Autowired
    private SeasonService seasonService;

    // 1. 新增赛季
    @PostMapping
    public Result add(@RequestBody @Validated Season season) {
        seasonService.add(season);
        return Result.success();
    }

    // 2. 获取赛季列表（含分页和按名称模糊搜索）
    @GetMapping
    public Result<PageBean<Season>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer pageNum,
            @RequestParam(defaultValue = "5") @Min(value = 1, message = "每页条数必须大于等于1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize,
            @RequestParam(required = false) String name
    ) {
        PageBean<Season> pb = seasonService.list(pageNum, pageSize, name);
        return Result.success(pb);
    }

    // 3. 查询赛季详情
    @GetMapping("/{id}")
    public Result<Season> detail(@PathVariable @NotNull(message = "赛季ID不能为空") Integer id) {
        Season season = seasonService.findById(id);
        if (season == null) {
            return Result.error("赛季不存在");
        }
        return Result.success(season);
    }

    // 4. 修改赛季
    @PutMapping
    public Result update(@RequestBody @Validated Season season) {
        if (seasonService.update(season) == 0) {
            return Result.error("赛季不存在");
        }
        return Result.success();
    }

    // 5. 删除赛季
    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable @NotNull(message = "赛季ID不能为空") Integer id) {
        if (seasonService.deleteById(id) == 0) {
            return Result.error("赛季不存在");
        }
        return Result.success();
    }
}
