package com.zhuyicheng.hoops.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhuyicheng.hoops.mapper.PlayerGameStatMapper;
import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.PlayerGameStat;
import com.zhuyicheng.hoops.service.PlayerGameStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerGameStatServiceImpl implements PlayerGameStatService {

    @Autowired
    private PlayerGameStatMapper playerGameStatMapper;

    @Override
    public void add(PlayerGameStat stat) {
        playerGameStatMapper.add(stat);
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
    public PlayerGameStat findById(Long id) {
        return playerGameStatMapper.findById(id);
    }

    @Override
    public int update(PlayerGameStat stat) {
        return playerGameStatMapper.update(stat);
    }

    @Override
    public int deleteById(Long id) {
        return playerGameStatMapper.deleteById(id);
    }
}
