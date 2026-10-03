package com.zhuyicheng.hoops.controller;

import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.PlayerGameStat;
import com.zhuyicheng.hoops.pojo.PlayerStatSummary;
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

    // 3. 球员汇总统计（场均数据与命中率）
    // 明细分页接口单页最多 100 条，前端把第一页当全量去平均会算错，场均类指标走这个接口。
    // 路径是字面量 /summary，PathPattern 会优先于 /{id} 匹配，不会被下面的详情接口截走。
    @GetMapping("/summary")
    public Result<PlayerStatSummary> summary(
            @RequestParam(required = false) @NotNull(message = "球员ID不能为空") Integer playerId,
            @RequestParam(required = false) Integer seasonId) {
        PlayerStatSummary summary = playerGameStatService.summary(playerId, seasonId);
        if (summary == null) {
            // 「该球员在该赛季没有比赛」是正常的空结果而不是错误，
            // 返回 code 0 + null，让前端展示「暂无数据」；返回 code 1 会弹错误提示
            return Result.success(null);
        }
        return Result.success(summary);
    }

    // 4. 查询球员单场数据详情
    @GetMapping("/{id}")
    public Result<PlayerGameStat> detail(@PathVariable @NotNull(message = "记录ID不能为空") Long id) {
        PlayerGameStat stat = playerGameStatService.findById(id);
        if (stat == null) {
            return Result.error("记录不存在");
        }
        return Result.success(stat);
    }

    // 5. 修改球员单场数据
    @PutMapping
    public Result update(@RequestBody @Validated PlayerGameStat stat) {
        if (playerGameStatService.update(stat) == 0) {
            return Result.error("记录不存在");
        }
        return Result.success();
    }

    // 6. 删除球员单场数据
    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable @NotNull(message = "记录ID不能为空") Long id) {
        if (playerGameStatService.deleteById(id) == 0) {
            return Result.error("记录不存在");
        }
        return Result.success();
    }
}
