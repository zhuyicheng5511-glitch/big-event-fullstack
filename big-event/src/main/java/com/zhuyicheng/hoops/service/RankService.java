package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.RankItem;

import java.util.List;

public interface RankService {

    List<RankItem> pointsRank(Integer seasonId, Integer teamId, String position, Integer limit);

    void evictPointsRankCache();
}
