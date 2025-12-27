package com.wxc.oj.service;

import com.wxc.oj.model.po.User;
import com.wxc.oj.model.vo.dayRank.DailyRankVO;
import com.wxc.oj.utils.DateUtils;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.wxc.oj.constant.RedisConstant.AC_RANK_KEY;

@Service
public class RankService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Resource
    private UserService userService;

    public List<DailyRankVO> getTodayTop10() {
        String currentDateStr = DateUtils.getCurrentDateStr();
        String key = AC_RANK_KEY + currentDateStr;
        // score 从高到低，取前 10
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet()
                        .reverseRangeWithScores(key, 0, 9);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        // 保持原始顺序
        List<ZSetOperations.TypedTuple<Object>> tuplesList = new ArrayList<>(tuples);

        // 收集top10的 userId，，避免循环中频繁查询数据库
        List<Long> userIds = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuplesList) {
            if (tuple == null || tuple.getValue() == null) continue;
            try {
                Long userId = Long.valueOf(String.valueOf(tuple.getValue()));
                userIds.add(userId);
            } catch (NumberFormatException e) {
                // 忽略无法解析的值
            }
        }
        // 批量查询用户信息，形成map
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listByIds(userIds);
            if (users != null && !users.isEmpty()) {
                userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
            }
        }

        List<DailyRankVO> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<Object> tuple : tuplesList) {
            if (tuple == null || tuple.getValue() == null) continue;
            Long userId;
            try {
                userId = Long.valueOf(String.valueOf(tuple.getValue()));
            } catch (NumberFormatException e) {
                continue;
            }
            DailyRankVO vo = new DailyRankVO();
            vo.setRank(rank++);
            vo.setUserId(userId);
            User user = userMap.get(userId);
            vo.setUserAccount(user == null ? null : user.getUserAccount());
            Number score = tuple.getScore();
            vo.setAcCount(score == null ? 0 : score.intValue());
            result.add(vo);
        }

        return result;
    }
}
