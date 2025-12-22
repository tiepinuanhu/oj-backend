package com.wxc.oj.constant;

/**
 * @author wangxinchao
 * @date 2025/11/30 21:08
 */
public class RedisConstant {
    public static String CACHE_PROBLEM_KEY = "oj:cache:problem:"; // +  pid
    public static String CACHE_USER_KEY = "oj:cache:user:"; // +  uid / token
    public static String CONTEST_KEY = "oj:cache:contest:"; // +  contest id 缓存比赛状态
    public static String CONTEST_PROBLEM_KEY = "oj:cache:contest:problem:"; // problem index in contest 缓存比赛中的题目


    public static final String USER_RANK_KEY = "oj:leaderboard:user:";

    // Redis Key命名规范
    public static final  String dailyRankKey = "oj:leaderboard:daily:";
//    public static final  String weeklyRankKey = "leaderboard:weekly:";
//    public static final  String monthlyRankKey = "leaderboard:monthly:";



    public static final String AC_PROBLEMS_KEY = "oj:ac:problems:";
    public static final String AC_RANK = "oj:ac:rank:";

}
