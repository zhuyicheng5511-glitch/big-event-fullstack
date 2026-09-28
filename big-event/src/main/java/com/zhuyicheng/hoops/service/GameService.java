package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.Game;
import com.zhuyicheng.hoops.pojo.PageBean;

public interface GameService {
    // 新增比赛
    void add(Game game);

    // 条件分页查询
    PageBean<Game> list(Integer pageNum, Integer pageSize, Integer seasonId, Integer teamId, String status);

    // 根据ID查询比赛详情
    Game findById(Integer id);

    // 修改比赛，返回受影响行数（0 = 比赛不存在）
    int update(Game game);

    // 删除比赛，返回受影响行数（0 = 比赛不存在）
    int deleteById(Integer id);
}
