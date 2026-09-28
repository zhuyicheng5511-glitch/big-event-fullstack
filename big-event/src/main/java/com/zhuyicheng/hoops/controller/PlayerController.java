package com.zhuyicheng.hoops.controller;

import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Player;
import com.zhuyicheng.hoops.pojo.Result;
import com.zhuyicheng.hoops.service.PlayerService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/player")
@Validated
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    // 1. 新增球员
    @PostMapping
    public Result add(@RequestBody @Validated Player player) {
        playerService.add(player);
        return Result.success();
    }

    // 2. 获取球员列表（含分页、按名称模糊搜索、按球队筛选）
    @GetMapping
    public Result<PageBean<Player>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer pageNum,
            @RequestParam(defaultValue = "5") @Min(value = 1, message = "每页条数必须大于等于1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer teamId
    ) {
        PageBean<Player> pb = playerService.list(pageNum, pageSize, name, teamId);
        return Result.success(pb);
    }

    // 3. 查询球员详情
    @GetMapping("/{id}")
    public Result<Player> detail(@PathVariable @NotNull(message = "球员ID不能为空") Integer id) {
        Player player = playerService.findById(id);
        if (player == null) {
            return Result.error("球员不存在");
        }
        return Result.success(player);
    }

    // 4. 修改球员
    @PutMapping
    public Result update(@RequestBody @Validated Player player) {
        if (playerService.update(player) == 0) {
            return Result.error("球员不存在");
        }
        return Result.success();
    }

    // 5. 删除球员
    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable @NotNull(message = "球员ID不能为空") Integer id) {
        if (playerService.deleteById(id) == 0) {
            return Result.error("球员不存在");
        }
        return Result.success();
    }

    // 处理外键约束冲突（如关联球队不存在，或球员被比赛数据引用时）
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        return Result.error("操作失败：关联数据不存在或存在冲突");
    }
}
