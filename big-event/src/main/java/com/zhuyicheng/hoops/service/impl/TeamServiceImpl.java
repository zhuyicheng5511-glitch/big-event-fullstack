package com.zhuyicheng.hoops.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhuyicheng.hoops.mapper.TeamMapper;
import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Team;
import com.zhuyicheng.hoops.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamServiceImpl implements TeamService {

    @Autowired
    private TeamMapper teamMapper;

    @Override
    public void add(Team team) {
        teamMapper.add(team);
    }

    @Override
    public PageBean<Team> list(Integer pageNum, Integer pageSize, String keyword) {
        PageBean<Team> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Team> list = teamMapper.list(keyword);
        Page<Team> p = (Page<Team>) list;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    public Team findById(Integer id) {
        return teamMapper.findById(id);
    }

    @Override
    public int update(Team team) {
        return teamMapper.update(team);
    }

    @Override
    public int deleteById(Integer id) {
        return teamMapper.deleteById(id);
    }
}
