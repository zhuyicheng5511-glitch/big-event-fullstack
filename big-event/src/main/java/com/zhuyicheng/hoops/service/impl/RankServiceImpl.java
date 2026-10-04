package com.zhuyicheng.hoops.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuyicheng.hoops.mapper.RankMapper;
import com.zhuyicheng.hoops.pojo.RankItem;
import com.zhuyicheng.hoops.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RankServiceImpl implements RankService {

    @Autowired
    private RankMapper rankMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_PREFIX = "rank:points:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration EMPTY_CACHE_TTL = Duration.ofMinutes(1);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public List<RankItem> pointsRank(Integer seasonId, Integer teamId, String position, Integer limit) {
        String key = buildCacheKey(seasonId, teamId, position, limit);
        String cached = stringRedisTemplate.opsForValue().get(key);

        if (cached != null) {
            if (cached.isEmpty()) {
                return Collections.emptyList();
            }
            try {
                return MAPPER.readValue(cached, new TypeReference<List<RankItem>>() {});
            } catch (Exception e) {
                // 反序列化失败则回数据库查
                stringRedisTemplate.delete(key);
            }
        }

        List<RankItem> list = rankMapper.pointsRank(seasonId, teamId, position, limit);

        try {
            String value = list == null || list.isEmpty() ? "" : MAPPER.writeValueAsString(list);
            Duration ttl = list == null || list.isEmpty() ? EMPTY_CACHE_TTL : CACHE_TTL;
            stringRedisTemplate.opsForValue().set(key, value, ttl.getSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            // 忽略缓存写入失败
        }

        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public void evictPointsRankCache() {
        // 用 SCAN 游标分批遍历代替 KEYS：KEYS 一次性扫描整个 keyspace 并阻塞 Redis 单线程，
        // key 多时会拖垮整个实例；SCAN 分小批迭代、攒批删除，对线上影响最小。
        ScanOptions options = ScanOptions.scanOptions().match(CACHE_PREFIX + "*").count(200).build();
        List<String> batch = new ArrayList<>(200);
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 200) {
                    stringRedisTemplate.delete(batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            stringRedisTemplate.delete(batch);
        }
    }

    private String buildCacheKey(Integer seasonId, Integer teamId, String position, Integer limit) {
        return CACHE_PREFIX + seasonId + ":"
                + (teamId == null ? "all" : teamId) + ":"
                + (position == null ? "all" : position) + ":"
                + limit;
    }
}
