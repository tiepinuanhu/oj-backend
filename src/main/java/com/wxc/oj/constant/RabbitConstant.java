package com.wxc.oj.constant;

/**
 * @author wangxinchao
 * @date 2025/12/14 11:13
 */
public class RabbitConstant {



    public static final String SUBMISSION_EXCHANGE = "oj.submission.exchange";
    public static final String SUBMISSION_QUEUE = "oj.submission.queue";
    public static final String SUBMISSION_ROUTING_KEY = "oj.submission.key";



    //    public static final String PROBLEM_EXCHANGE = "problem_exchange";
    public static final String SUBMISSION_STATUS_EXCHANGE = "oj.submission.status.exchange";
    public static final String SUBMISSION_STATUS_PROBLEM_QUEUE = "oj.submission.problem.queue";
    public static final String SUBMISSION_STATUS_AC_QUEUE = "oj.ac.rank.queue";
    public static final String SUBMISSION_STATUS_TOPIC = "oj.submission.status";
//    public static final String SUBMISSION_STATUS_KEY = "submission_status_key";


    public static final String CONTEST_TIME_EXCHANGE = "oj.contest.time.delay.exchange";
    public static final String CONTEST_PUBLISH_QUEUE = "oj.timePublish.delay.queue";
    public static final String CONTEST_FINISH_QUEUE = "oj.timeFinish.delay.queue";
    public static final String CONTEST_PUBLISH_KEY = "oj.contest.publish";
    public static final String CONTEST_FINISH_KEY = "oj.contest.finish";
}
