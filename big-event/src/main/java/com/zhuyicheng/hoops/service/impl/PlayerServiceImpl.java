package com.zhuyicheng.hoops.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhuyicheng.hoops.mapper.PlayerMapper;
import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Player;
import com.zhuyicheng.hoops.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerServiceImpl implements PlayerService {

    @Autowired
    private PlayerMapper playerMapper;

    @Override
    public void add(Player player) {
        playerMapper.add(player);
    }

    @Override
    public PageBean<Player> list(Integer pageNum, Integer pageSize, String name, Integer teamId) {
        PageBean<Player> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Player> list = playerMapper.list(name, teamId);
        Page<Player> p = (Page<Player>) list;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    public Player findById(Integer id) {
        return playerMapper.findById(id);
    }

    @Override
    public int update(Player player) {
        return playerMapper.update(player);
    }

    @Override
    public int deleteById(Integer id) {
        return playerMapper.deleteById(id);
    }
}
