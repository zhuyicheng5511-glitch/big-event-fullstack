package com.zhuyicheng.hoops.controller;

import com.zhuyicheng.hoops.pojo.Result;
import com.zhuyicheng.hoops.pojo.ZoneStat;
import com.zhuyicheng.hoops.service.ShotChartService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shot")
@Validated
public class ShotChartController {

    @Autowired
    private ShotChartService shotChartService;

    @GetMapping("/chart")
    // 两个 id 都必须是 required = false，否则参数缺失时 Spring 会在进入方法之前抛异常，
    // 下面 @NotNull 的提示语不会生效（会被全局异常处理器兜成「服务器繁忙」）。
    public Result<List<ZoneStat>> chart(@RequestParam(required = false) @NotNull(message = "球员ID不能为空") Integer playerId,
                                        @RequestParam(required = false) @NotNull(message = "赛季ID不能为空") Integer seasonId,
                                        @RequestParam(required = false) String zone) {
        List<ZoneStat> list = shotChartService.chartByPlayerAndSeason(playerId, seasonId, zone);
        return Result.success(list);
    }
}
