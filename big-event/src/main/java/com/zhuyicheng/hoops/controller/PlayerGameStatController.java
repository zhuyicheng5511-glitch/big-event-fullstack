package com.zhuyicheng.hoops.controller;

import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.PlayerGameStat;
import com.zhuyicheng.hoops.pojo.Result;
import com.zhuyicheng.hoops.service.PlayerGameStatService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/player-game-stat")
@Validated
public class PlayerGameStatController {

    @Autowired
    private PlayerGameStatService playerGameStatService;

    // 1. 新增球员单场数据
    @PostMapping
    public Result add(@RequestBody @Validated PlayerGameStat stat) {
        playerGameStatService.add(stat);
        return Result.success();
    }

    // 2. 获取球员单场数据列表（含分页和按比赛/球员/球队筛选）
    @GetMapping
    public Result<PageBean<PlayerGameStat>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer pageNum,
            @RequestParam(defaultValue = "5") @Min(value = 1, message = "每页条数必须大于等于1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize,
            @RequestParam(required = false) Integer gameId,
            @RequestParam(required = false) Integer playerId,
            @RequestParam(required = false) Integer teamId) {
        PageBean<PlayerGameStat> pb = playerGameStatService.list(pageNum, pageSize, gameId, playerId, teamId);
        return Result.success(pb);
    }

    // 3. 查询球员单场数据详情
    @GetMapping("/{id}")
    public Result<PlayerGameStat> detail(@PathVariable @NotNull(message = "记录ID不能为空") Long id) {
        PlayerGameStat stat = playerGameStatService.findById(id);
        if (stat == null) {
            return Result.error("记录不存在");
        }
        return Result.success(stat);
    }

    // 4. 修改球员单场数据
    @PutMapping
    public Result update(@RequestBody @Validated PlayerGameStat stat) {
        if (playerGameStatService.update(stat) == 0) {
            return Result.error("记录不存在");
        }
        return Result.success();
    }

    // 5. 删除球员单场数据
    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable @NotNull(message = "记录ID不能为空") Long id) {
        if (playerGameStatService.deleteById(id) == 0) {
            return Result.error("记录不存在");
        }
        return Result.success();
    }
}
