package com.zhuyicheng.hoops.service.impl;

import com.zhuyicheng.hoops.mapper.ShotChartMapper;
import com.zhuyicheng.hoops.pojo.ZoneStat;
import com.zhuyicheng.hoops.service.ShotChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShotChartServiceImpl implements ShotChartService {

    @Autowired
    private ShotChartMapper shotChartMapper;

    private static final Map<String, double[]> ZONE_CENTERS = new HashMap<>();

    static {
        ZONE_CENTERS.put("篮下", new double[]{25.0, 2.0});
        ZONE_CENTERS.put("油漆区", new double[]{25.0, 7.0});
        ZONE_CENTERS.put("中距离", new double[]{25.0, 15.0});
        ZONE_CENTERS.put("左底角三分", new double[]{2.5, 9.0});
        ZONE_CENTERS.put("右底角三分", new double[]{47.5, 9.0});
        ZONE_CENTERS.put("左翼三分", new double[]{7.5, 26.5});
        ZONE_CENTERS.put("右翼三分", new double[]{42.5, 26.5});
        ZONE_CENTERS.put("弧顶三分", new double[]{25.0, 30.0});
    }

    @Override
    public List<ZoneStat> chartByPlayerAndSeason(Integer playerId, Integer seasonId, String zone) {
        List<ZoneStat> list = shotChartMapper.chartByPlayerAndSeason(playerId, seasonId, zone);
        if (list == null) {
            return null;
        }
        for (ZoneStat stat : list) {
            double[] center = ZONE_CENTERS.get(stat.getZone());
            if (center != null) {
                stat.setCenterX(center[0]);
                stat.setCenterY(center[1]);
            }
            // fgPct 从数据库已经是 BigDecimal，避免 null
            if (stat.getFgPct() == null) {
                stat.setFgPct(BigDecimal.ZERO);
            }
        }
        return list;
    }
}
