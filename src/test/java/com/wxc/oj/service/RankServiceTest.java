package com.wxc.oj.service;

import com.wxc.oj.model.vo.dayRank.DailyRankVO;
import com.wxc.oj.utils.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.TestPropertySource;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.yaml")
class RankServiceTest {

    @Resource
    private RankService rankService;



    @Test
    void testGetTodayTop10() {
        List<DailyRankVO> todayTop10 = rankService.getTodayTop10();
        System.out.println("todayTop10 = " + todayTop10);
    }

    @Test
    void testGetTodayTop10WithEmptyData() {

    }
}