package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.ZoneStat;

import java.util.List;

public interface ShotChartService {

    List<ZoneStat> chartByPlayerAndSeason(Integer playerId, Integer seasonId, String zone);
}
