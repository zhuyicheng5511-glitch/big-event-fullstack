package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Player;

public interface PlayerService {
    // 新增球员
    void add(Player player);

    // 条件分页查询
    PageBean<Player> list(Integer pageNum, Integer pageSize, String name, Integer teamId);

    // 根据ID查询球员详情
    Player findById(Integer id);

    // 修改球员，返回受影响行数（0 = 球员不存在）
    int update(Player player);

    // 删除球员，返回受影响行数（0 = 球员不存在）
    int deleteById(Integer id);
}
