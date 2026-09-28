package com.zhuyicheng.hoops.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhuyicheng.hoops.mapper.SeasonMapper;
import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Season;
import com.zhuyicheng.hoops.service.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeasonServiceImpl implements SeasonService {

    @Autowired
    private SeasonMapper seasonMapper;

    @Override
    public void add(Season season) {
        seasonMapper.add(season);
    }

    @Override
    public PageBean<Season> list(Integer pageNum, Integer pageSize, String name) {
        PageBean<Season> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Season> list = seasonMapper.list(name);
        Page<Season> p = (Page<Season>) list;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    public Season findById(Integer id) {
        return seasonMapper.findById(id);
    }

    @Override
    public int update(Season season) {
        return seasonMapper.update(season);
    }

    @Override
    public int deleteById(Integer id) {
        return seasonMapper.deleteById(id);
    }
}
