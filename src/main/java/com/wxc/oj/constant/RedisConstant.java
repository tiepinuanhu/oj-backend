package com.wxc.oj.constant;

/**
 * @author wangxinchao
 * @date 2025/11/30 21:08
 */
public class RedisConstant {
    public static String PROBLEM_KEY = "oj:cache:problem:"; // +  pid
    public static String USER_KEY = "oj:cache:user:"; // +  uid / token
    public static String CONTEST_KEY = "oj:cache:contest:"; // +  contest id 缓存比赛状态
    public static String CONTEST_PROBLEM_KEY = "oj:cache:contest:problem:"; // problem index in contest 缓存比赛中的题目

}
