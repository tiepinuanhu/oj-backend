# 数据库初始化

-- 创建库
create database if not exists db_oj;

-- 切换库
use db_oj;

-- 用户表
CREATE TABLE `user` (
    `id` bigint NOT NULL COMMENT '用户ID（雪花算法生成）',
    `user_account` varchar(50) NOT NULL COMMENT '用户账号（登录用）',
    `user_password` varchar(255) NOT NULL COMMENT '用户密码（加密存储）',
    `user_name` varchar(100) NOT NULL COMMENT '用户昵称',
    `union_id` varchar(255) DEFAULT NULL COMMENT '第三方登录联合ID（如微信、QQ等）',
    `user_avatar` varchar(255) DEFAULT NULL COMMENT '用户头像URL',
    `user_profile` varchar(500) DEFAULT NULL COMMENT '用户简介',
    `user_role` int NOT NULL COMMENT '用户角色（0-普通用户，1-管理员等）',
    `create_time` datetime NOT NULL COMMENT '创建时间（注册时间）',
    `update_time` datetime NOT NULL COMMENT '更新时间（信息最后修改时间）',
    `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_account` (`user_account`) COMMENT '确保用户账号唯一（不可重复注册）',
    KEY `idx_user_role` (`user_role`) COMMENT '按角色筛选用户（如查询所有管理员）',
    KEY `idx_create_time` (`create_time`) COMMENT '按注册时间查询用户'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表（存储系统用户基本信息）';
-- 题目表
CREATE TABLE `problem` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键自增',
    `title` varchar(255) NOT NULL COMMENT '题目标题',
    `content` text NOT NULL COMMENT '题目内容（包含题干、输入输出描述、示例等）',
    `level` int NOT NULL COMMENT '题目难度（如1-简单、2-中等、3-困难）',
    `submitted_num` int NOT NULL DEFAULT 0 COMMENT '总提交次数',
    `accepted_num` int NOT NULL DEFAULT 0 COMMENT '通过次数',
    `judge_config` text DEFAULT NULL COMMENT '评测配置（JSON格式，如时间限制、内存限制等）',
    `user_id` bigint NOT NULL COMMENT '题目创建者ID（关联用户表）',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_time` datetime NOT NULL COMMENT '更新时间',
    `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0-未删除，1-已删除）',
    `is_public` int NOT NULL DEFAULT 0 COMMENT '是否公开（0-私有，1-公开）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`) COMMENT '按创建者ID查询题目',
    KEY `idx_level` (`level`) COMMENT '按难度筛选题目',
    KEY `idx_is_public` (`is_public`) COMMENT '筛选公开/私有题目',
    KEY `idx_title` (`title`) COMMENT '按标题模糊查询题目'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表（存储编程题的基本信息）';
-- 提交表
CREATE TABLE `submission` (
    `id` bigint NOT NULL COMMENT '提交记录ID（雪花算法生成）',
    `user_id` bigint NOT NULL COMMENT '提交用户ID',
    `problem_id` bigint NOT NULL COMMENT '题目ID',
    `source_code` text NOT NULL COMMENT '提交的源代码',
    `submission_result` text DEFAULT NULL COMMENT '提交结果（JSON字符串）',
    `status` int DEFAULT NULL COMMENT '评测状态码（如0-待评测、1-通过等）',
    `status_description` varchar(255) DEFAULT NULL COMMENT '评测状态描述（如"Accepted"、"Wrong Answer"）',
    `language` varchar(50) NOT NULL COMMENT '编程语言（如java、python、c++等）',
    `create_time` datetime NOT NULL COMMENT '创建时间（提交时间）',
    `update_time` datetime NOT NULL COMMENT '更新时间（最后评测时间）',
    `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`) COMMENT '按用户ID查询提交记录',
    KEY `idx_problem_id` (`problem_id`) COMMENT '按题目ID查询提交记录',
    KEY `idx_create_time` (`create_time`) COMMENT '按提交时间排序查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码提交记录表';
-- 比赛表
CREATE TABLE `contest` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键自增',
    `title` varchar(255) NOT NULL COMMENT '比赛标题',
    `description` text NOT NULL COMMENT '比赛描述（包含规则、说明等）',
    `start_time` datetime NOT NULL COMMENT '比赛开始时间',
    `duration` int NOT NULL COMMENT '比赛时长（单位：分钟）',
    `status` int NOT NULL COMMENT '比赛状态（如0-未开始、1-进行中、2-已结束）',
    `is_public` int NOT NULL DEFAULT 0 COMMENT '是否公开（0-私有，1-公开）',
    `created_time` datetime NOT NULL COMMENT '创建时间',
    `updated_time` datetime NOT NULL COMMENT '更新时间',
    `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0-未删除，1-已删除）',
    `host_id` bigint NOT NULL COMMENT '主办方/创建者ID（关联用户表）',
    `can_register` int NOT NULL DEFAULT 1 COMMENT '是否允许报名（0-不允许，1-允许）',
    PRIMARY KEY (`id`),
    KEY `idx_host_id` (`host_id`) COMMENT '按创建者ID查询其创建的比赛',
    KEY `idx_status` (`status`) COMMENT '按比赛状态筛选（如查询进行中的比赛）',
    KEY `idx_is_public` (`is_public`) COMMENT '筛选公开/私有比赛',
    KEY `idx_start_time` (`start_time`) COMMENT '按开始时间排序查询比赛'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛表（存储编程比赛的基本信息）';
-- 比赛题目表
CREATE TABLE `contest_problem` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键自增',
    `contest_id` bigint NOT NULL COMMENT '比赛ID（关联比赛表）',
    `problem_id` bigint NOT NULL COMMENT '题目ID（关联题目表）',
    `pindex` int NOT NULL COMMENT '题目在比赛中的序号（如A、B题的索引，用于排序）',
    `created_time` datetime NOT NULL COMMENT '创建时间（题目添加到比赛的时间）',
    `updated_time` datetime NOT NULL COMMENT '更新时间（题目信息最后修改时间）',
    `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0-未删除，1-已删除）',
    `full_score` int NOT NULL COMMENT '比赛中该题目的满分分值',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contest_problem` (`contest_id`, `problem_id`) COMMENT '确保一个题目在同一比赛中只出现一次',
    UNIQUE KEY `uk_contest_pindex` (`contest_id`, `pindex`) COMMENT '确保同一比赛中题目序号不重复',
    KEY `idx_contest_id` (`contest_id`) COMMENT '按比赛ID查询包含的题目',
    KEY `idx_problem_id` (`problem_id`) COMMENT '按题目ID查询所属比赛'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛与题目关联表（记录比赛包含的题目及相关配置）';
-- 比赛报名表
CREATE TABLE `contest_registration` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键自增',
    `user_id` bigint NOT NULL COMMENT '用户ID（关联用户表，报名者）',
    `contest_id` bigint NOT NULL COMMENT '比赛ID（关联比赛表，报名的比赛）',
    `created_time` datetime NOT NULL COMMENT '报名时间',
    `updated_time` datetime NOT NULL COMMENT '更新时间（如报名状态变更时间）',
    `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_contest` (`user_id`, `contest_id`) COMMENT '确保同一用户不能重复报名同一比赛',
    KEY `idx_user_id` (`user_id`) COMMENT '按用户ID查询其报名的所有比赛',
    KEY `idx_contest_id` (`contest_id`) COMMENT '按比赛ID查询所有报名用户'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛报名表（记录用户报名比赛的信息）';
-- 比赛提交表
CREATE TABLE `contest_submission` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id主键自增',
    `contest_id` bigint NOT NULL COMMENT '比赛ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `problem_id` bigint NOT NULL COMMENT '题目ID',
    `submission_time` datetime NOT NULL COMMENT '提交时间',
    `source_code` text NOT NULL COMMENT '源代码',
    `language` varchar(50) NOT NULL COMMENT '编程语言（如java、python等）',
    `submission_result` varchar(255) DEFAULT NULL COMMENT '提交结果概述',
    `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0-未删除，1-已删除）',
    `status` int DEFAULT NULL COMMENT '评测状态码（如0-待评测、1-通过等）',
    `status_description` varchar(255) DEFAULT NULL COMMENT '评测状态描述',
    `score` int DEFAULT NULL COMMENT '题目得分',
    `total_time` bigint DEFAULT NULL COMMENT '总耗时（毫秒）',
    `memory_used` bigint DEFAULT NULL COMMENT '内存使用量（字节）',
    `judge_case_results` text DEFAULT NULL COMMENT '各评测用例结果（通常为JSON格式）',
    PRIMARY KEY (`id`),
    KEY `idx_contest_id` (`contest_id`) COMMENT '按比赛ID查询提交记录',
    KEY `idx_user_id` (`user_id`) COMMENT '按用户ID查询提交记录',
    KEY `idx_problem_id` (`problem_id`) COMMENT '按题目ID查询提交记录',
    KEY `idx_submission_time` (`submission_time`) COMMENT '按提交时间排序查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛提交记录表';
-- 题目标签表
CREATE TABLE `tag` (
   `id` int NOT NULL AUTO_INCREMENT COMMENT '主键自增',
   `name` varchar(100) NOT NULL COMMENT '标签名称',
   `color` varchar(50) NOT NULL COMMENT '标签颜色（通常为十六进制色值或颜色名称）',
   PRIMARY KEY (`id`),
   KEY `idx_name` (`name`) COMMENT '按标签名称查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';