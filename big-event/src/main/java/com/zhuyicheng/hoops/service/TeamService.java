package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Team;

public interface TeamService {
    // 新增球队
    void add(Team team);

    // 条件分页查询
    PageBean<Team> list(Integer pageNum, Integer pageSize, String keyword);

    // 根据ID查询球队详情
    Team findById(Integer id);

    // 修改球队，返回受影响行数（0 = 球队不存在）
    int update(Team team);

    // 删除球队，返回受影响行数（0 = 球队不存在）
    int deleteById(Integer id);
}
