package com.zhuyicheng.hoops.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhuyicheng.hoops.mapper.GameMapper;
import com.zhuyicheng.hoops.pojo.Game;
import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameServiceImpl implements GameService {

    @Autowired
    private GameMapper gameMapper;

    @Override
    public void add(Game game) {
        gameMapper.add(game);
    }

    @Override
    public PageBean<Game> list(Integer pageNum, Integer pageSize, Integer seasonId, Integer teamId, String status) {
        PageBean<Game> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Game> list = gameMapper.list(seasonId, teamId, status);
        Page<Game> p = (Page<Game>) list;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    public Game findById(Integer id) {
        return gameMapper.findById(id);
    }

    @Override
    public int update(Game game) {
        return gameMapper.update(game);
    }

    @Override
    public int deleteById(Integer id) {
        return gameMapper.deleteById(id);
    }
}
