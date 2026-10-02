package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Shot;

public interface ShotService {
    // 新增
    void add(Shot shot);

    // 条件分页查询
    PageBean<Shot> list(Integer pageNum, Integer pageSize, Integer gameId, Integer playerId, String zone);

    // 根据ID查询详情
    Shot findById(Long id);

    // 修改，返回受影响行数
    int update(Shot shot);

    // 删除，返回受影响行数
    int deleteById(Long id);
}
