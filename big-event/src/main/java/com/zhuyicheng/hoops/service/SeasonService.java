package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.PageBean;
import com.zhuyicheng.hoops.pojo.Season;

public interface SeasonService {
    // 新增赛季
    void add(Season season);

    // 条件分页查询
    PageBean<Season> list(Integer pageNum, Integer pageSize, String name);

    // 根据ID查询赛季详情
    Season findById(Integer id);

    // 修改赛季，返回受影响行数（0 = 赛季不存在）
    int update(Season season);

    // 删除赛季，返回受影响行数（0 = 赛季不存在）
    int deleteById(Integer id);
}
