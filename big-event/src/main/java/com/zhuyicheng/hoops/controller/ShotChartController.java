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
    public Result<List<ZoneStat>> chart(@RequestParam @NotNull(message = "球员ID不能为空") Integer playerId,
                                        @RequestParam @NotNull(message = "赛季ID不能为空") Integer seasonId,
                                        @RequestParam(required = false) String zone) {
        List<ZoneStat> list = shotChartService.chartByPlayerAndSeason(playerId, seasonId, zone);
        return Result.success(list);
    }
}
