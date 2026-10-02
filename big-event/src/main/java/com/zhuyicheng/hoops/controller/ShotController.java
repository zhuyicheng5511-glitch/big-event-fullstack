package com.zhuyicheng.hoops.controller;

import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Result;
import com.zhuyicheng.hoops.pojo.Shot;
import com.zhuyicheng.hoops.service.ShotService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shot")
@Validated
public class ShotController {

    @Autowired
    private ShotService shotService;

    // 1. 新增投篮事件
    @PostMapping
    public Result add(@RequestBody @Validated Shot shot) {
        shotService.add(shot);
        return Result.success();
    }

    // 2. 获取投篮事件列表（含分页和按比赛/球员/区域筛选）
    @GetMapping
    public Result<PageBean<Shot>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer pageNum,
            @RequestParam(defaultValue = "5") @Min(value = 1, message = "每页条数必须大于等于1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize,
            @RequestParam(required = false) Integer gameId,
            @RequestParam(required = false) Integer playerId,
            @RequestParam(required = false) String zone) {
        PageBean<Shot> pb = shotService.list(pageNum, pageSize, gameId, playerId, zone);
        return Result.success(pb);
    }

    // 3. 查询投篮事件详情
    @GetMapping("/{id}")
    public Result<Shot> detail(@PathVariable @NotNull(message = "记录ID不能为空") Long id) {
        Shot shot = shotService.findById(id);
        if (shot == null) {
            return Result.error("记录不存在");
        }
        return Result.success(shot);
    }

    // 4. 修改投篮事件
    @PutMapping
    public Result update(@RequestBody @Validated Shot shot) {
        if (shotService.update(shot) == 0) {
            return Result.error("记录不存在");
        }
        return Result.success();
    }

    // 5. 删除投篮事件
    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable @NotNull(message = "记录ID不能为空") Long id) {
        if (shotService.deleteById(id) == 0) {
            return Result.error("记录不存在");
        }
        return Result.success();
    }
}
