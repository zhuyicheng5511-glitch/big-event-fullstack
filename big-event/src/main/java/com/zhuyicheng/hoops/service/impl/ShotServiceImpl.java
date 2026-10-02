package com.zhuyicheng.hoops.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhuyicheng.hoops.mapper.ShotMapper;
import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Shot;
import com.zhuyicheng.hoops.service.ShotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShotServiceImpl implements ShotService {

    @Autowired
    private ShotMapper shotMapper;

    @Override
    public void add(Shot shot) {
        shotMapper.add(shot);
    }

    @Override
    public PageBean<Shot> list(Integer pageNum, Integer pageSize, Integer gameId, Integer playerId, String zone) {
        PageBean<Shot> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Shot> list = shotMapper.list(gameId, playerId, zone);
        Page<Shot> p = (Page<Shot>) list;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    public Shot findById(Long id) {
        return shotMapper.findById(id);
    }

    @Override
    public int update(Shot shot) {
        return shotMapper.update(shot);
    }

    @Override
    public int deleteById(Long id) {
        return shotMapper.deleteById(id);
    }
}
