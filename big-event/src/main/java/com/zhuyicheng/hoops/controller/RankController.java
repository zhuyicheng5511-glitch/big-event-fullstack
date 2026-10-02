package com.zhuyicheng.hoops.controller;

import com.zhuyicheng.hoops.pojo.RankItem;
import com.zhuyicheng.hoops.pojo.Result;
import com.zhuyicheng.hoops.service.RankService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rank")
@Validated
public class RankController {

    @Autowired
    private RankService rankService;

    @GetMapping("/points")
    public Result<List<RankItem>> pointsRank(
            @RequestParam @NotNull(message = "赛季ID不能为空") Integer seasonId,
            @RequestParam(required = false) Integer teamId,
            @RequestParam(required = false) String position,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit必须大于等于1") @Max(value = 100, message = "limit最大为100") Integer limit) {
        List<RankItem> list = rankService.pointsRank(seasonId, teamId, position, limit);
        return Result.success(list);
    }
}
