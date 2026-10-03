package com.zhuyicheng.hoops.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhuyicheng.hoops.mapper.PlayerGameStatMapper;
import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.PlayerGameStat;
import com.zhuyicheng.hoops.pojo.PlayerStatSummary;
import com.zhuyicheng.hoops.service.PlayerGameStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhuyicheng.hoops.service.RankService;

import java.util.List;

@Service
public class PlayerGameStatServiceImpl implements PlayerGameStatService {

    @Autowired
    private PlayerGameStatMapper playerGameStatMapper;

    @Autowired
    private RankService rankService;

    @Override
    public void add(PlayerGameStat stat) {
        playerGameStatMapper.add(stat);
        rankService.evictPointsRankCache();
    }

    @Override
    public PageBean<PlayerGameStat> list(Integer pageNum, Integer pageSize, Integer gameId, Integer playerId, Integer teamId) {
        PageBean<PlayerGameStat> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<PlayerGameStat> list = playerGameStatMapper.list(gameId, playerId, teamId);
        Page<PlayerGameStat> p = (Page<PlayerGameStat>) list;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    public PlayerStatSummary summary(Integer playerId, Integer seasonId) {
        // 直接让数据库做聚合：明细分页单页上限 100 条，前端拿第一页当全量会算错场均数据
        return playerGameStatMapper.summary(playerId, seasonId);
    }

    @Override
    public PlayerGameStat findById(Long id) {
        return playerGameStatMapper.findById(id);
    }

    @Override
    public int update(PlayerGameStat stat) {
        int rows = playerGameStatMapper.update(stat);
        if (rows > 0) {
            rankService.evictPointsRankCache();
        }
        return rows;
    }

    @Override
    public int deleteById(Long id) {
        int rows = playerGameStatMapper.deleteById(id);
        if (rows > 0) {
            rankService.evictPointsRankCache();
        }
        return rows;
    }
}
