package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.PlayerGameStat;

public interface PlayerGameStatService {
    // 新增
    void add(PlayerGameStat stat);

    // 条件分页查询
    PageBean<PlayerGameStat> list(Integer pageNum, Integer pageSize, Integer gameId, Integer playerId, Integer teamId);

    // 根据ID查询详情
    PlayerGameStat findById(Long id);

    // 修改，返回受影响行数
    int update(PlayerGameStat stat);

    // 删除，返回受影响行数
    int deleteById(Long id);
}
