package com.zhuyicheng.hoops.controller;

import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Result;
import com.zhuyicheng.hoops.pojo.Team;
import com.zhuyicheng.hoops.service.TeamService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/team")
@Validated
public class TeamController {

    @Autowired
    private TeamService teamService;

    // 1. 新增球队
    @PostMapping
    public Result add(@RequestBody @Validated Team team) {
        teamService.add(team);
        return Result.success();
    }

    // 2. 获取球队列表（含分页和按名称/城市模糊搜索）
    @GetMapping
    public Result<PageBean<Team>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer pageNum,
            @RequestParam(defaultValue = "5") @Min(value = 1, message = "每页条数必须大于等于1") @Max(value = 100, message = "每页条数最大为100") Integer pageSize,
            @RequestParam(required = false) String keyword
    ) {
        PageBean<Team> pb = teamService.list(pageNum, pageSize, keyword);
        return Result.success(pb);
    }

    // 3. 查询球队详情
    @GetMapping("/{id}")
    public Result<Team> detail(@PathVariable @NotNull(message = "球队ID不能为空") Integer id) {
        Team team = teamService.findById(id);
        if (team == null) {
            return Result.error("球队不存在");
        }
        return Result.success(team);
    }

    // 4. 修改球队
    @PutMapping
    public Result update(@RequestBody @Validated Team team) {
        if (teamService.update(team) == 0) {
            return Result.error("球队不存在");
        }
        return Result.success();
    }

    // 5. 删除球队
    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable @NotNull(message = "球队ID不能为空") Integer id) {
        if (teamService.deleteById(id) == 0) {
            return Result.error("球队不存在");
        }
        return Result.success();
    }
}
