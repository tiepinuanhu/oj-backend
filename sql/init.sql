# 数据库初始化

-- 创建库
create database if not exists db_oj;

-- 切换库
use db_oj;

-- 用户表
create table user
(
    id            bigint                                 not null comment '用户ID（雪花算法生成）'
        primary key,
    user_account  varchar(50)                            not null comment '用户账号（登录用）',
    user_password varchar(255)                           not null comment '用户密码（加密存储）',
    user_name     varchar(100) default 'momo'            not null comment '用户昵称',
    union_id      varchar(255)                           null comment '第三方登录联合ID（如微信、QQ等）',
    user_avatar   varchar(255)                           null comment '用户头像URL',
    user_profile  varchar(500)                           null comment '用户简介',
    user_role     int          default 0                 not null comment '用户角色（0-普通用户，1-管理员等）',
    create_time   datetime     default (now())           not null comment '创建时间（注册时间）',
    update_time   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间（信息最后修改时间）',
    is_deleted    int          default 0                 not null comment '逻辑删除标志（0-未删除，1-已删除）',
    constraint uk_user_account
        unique (user_account) comment '确保用户账号唯一（不可重复注册）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  comment '用户表（存储系统用户基本信息）';
INSERT INTO db_oj.user (id, user_account, user_password, user_name, union_id, user_avatar, user_profile, user_role, create_time, update_time, is_deleted) VALUES (2002744707925106690, 'wxc', '5d789a466f021886cc60d6eebd109f26', 'momo', null, null, null, 1, '2025-12-21 14:15:23', '2025-12-21 14:15:59', 0);
INSERT INTO db_oj.user (id, user_account, user_password, user_name, union_id, user_avatar, user_profile, user_role, create_time, update_time, is_deleted) VALUES (2004743556063502337, 'jack', '5d789a466f021886cc60d6eebd109f26', 'momo', null, null, null, 0, '2025-12-27 02:38:03', '2025-12-27 02:38:03', 0);

-- 题目表
create table problem
(
    id            bigint auto_increment comment '主键自增'
        primary key,
    title         varchar(255)                       not null comment '题目标题',
    content       text                               not null comment '题目内容（包含题干、输入输出描述、示例等）',
    level         int                                not null comment '题目难度（如1-简单、2-中等、3-困难）',
    submitted_num int      default 0                 not null comment '总提交次数',
    accepted_num  int      default 0                 not null comment '通过次数',
    judge_config  text                               null comment '评测配置（JSON格式，如时间限制、内存限制等）',
    user_id       bigint                             not null comment '题目创建者ID（关联用户表）',
    create_time   datetime default (now())           not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_deleted    int      default 0                 not null comment '逻辑删除标志（0-未删除，1-已删除）',
    is_public     int      default 0                 not null comment '是否公开（0-私有，1-公开）',
    KEY `idx_user_id` (`user_id`) COMMENT '按创建者ID查询题目',
    KEY `idx_level` (`level`) COMMENT '按难度筛选题目',
    KEY `idx_is_public` (`is_public`) COMMENT '筛选公开/私有题目',
    KEY `idx_title` (`title`) COMMENT '按标题模糊查询题目'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4    comment '题目表（存储编程题的基本信息）';
INSERT INTO db_oj.problem (id, title, content, level, submitted_num, accepted_num, judge_config, user_id, create_time, update_time, is_deleted, is_public) VALUES (1, 'A+B Problem', '# P1001 A+B Problem

## 题目背景

**不熟悉算法竞赛的选手请看这里：**

算法竞赛中要求的输出格式中，**不能有多余的内容**，**这也包括了“请输入整数 $\\bm a$ 和 $\\bm b$” 这一类的提示用户输入信息的内容**。若包含了这些内容，将会被认为是 `Wrong Answer`，即洛谷上的 `WA`。在对比代码输出和标准输出时，系统将忽略每一行结尾的空格，以及最后一行之后多余的换行符。

若因此类问题出现本机似乎输出了正确的结果，但是实际提交结果为错误的现象，请勿认为是洛谷评测机出了问题，而是你的代码中可能存在多余的输出信息。用户可以参考在题目末尾提供的代码。

此外，**请善用进入 IDE 模式**，以避免不同平台的评测产生差异。

最后，请不要在对应的题目讨论区中发布自己的题解，请发布到题解区域中，否则将处以删除或禁言的处罚。若发现无法提交题解则表明本题题解数量过多，仍不应发布讨论。若您的做法确实与其他所有题解均不一样，请联系管理员添加题解。

## 题目描述

输入两个整数 $a, b$，输出它们的和（$|a|,|b| \\le {10}^9$）。

注意

1. Pascal 使用 `integer` 会爆掉哦！
2. 有负数哦！
3. C/C++ 的 main 函数必须是 `int` 类型。程序正常结束时的返回值必须是 0。这不仅对洛谷其他题目有效，而且也是 NOIP/CSP/NOI 比赛的要求！

好吧，同志们，我们就从这一题开始，向着大牛的路进发。

> 任何一个伟大的思想，都有一个微不足道的开始。

## 输入格式

两个以空格分开的整数。

## 输出格式

一个整数。

## 输入输出样例 #1

### 输入 #1

```
20 30
```

### 输出 #1

```
50
```

## 说明/提示

**广告**

洛谷出品的算法教材，帮助您更简单的学习基础算法。[【官方网店绝赞热卖中！】>>>](https://item.taobao.com/item.htm?id=637730514783)

[![](https://cdn.luogu.com.cn/upload/image_hosting/njc7dlng.png)](https://item.taobao.com/item.htm?id=637730514783)

**本题各种语言的程序范例：**

C
```c
#include <stdio.h>

int main()
{
    int a,b;
    scanf("%d%d",&a,&b);
    printf("%d\\n", a+b);
    return 0;
}
```
----------------

C++
```cpp
#include <iostream>
#include <cstdio>

using namespace std;

int main()
{
    int a,b;
    cin >> a >> b;
    cout << a+b << endl;
    return 0;
}
```
----------------

Pascal
```pascal
var a, b: longint;
begin
    readln(a,b);
    writeln(a+b);
end.
```
-----------------

Python 3

```python
s = input().split()
print(int(s[0]) + int(s[1]))
```
-----------------

Java
```java
import java.io.*;
import java.util.*;
public class Main {
    public static void main(String args[]) throws Exception {
        Scanner cin=new Scanner(System.in);
        int a = cin.nextInt(), b = cin.nextInt();
        System.out.println(a+b);
    }
}
```
-----------------

JavaScript （Node.js）

```javascript
const fs = require(\'fs\')
const data = fs.readFileSync(\'/dev/stdin\')
const result = data.toString(\'ascii\').trim().split(\' \').map(x => parseInt(x)).reduce((a, b) => a + b, 0)
console.log(result)
process.exit() // 请注意必须在出口点处加入此行
```

-----------------

Ruby

```ruby
a, b = gets.split.map(&:to_i)
print a+b
```

-----------------

PHP

```php
<?php
$input = trim(file_get_contents("php://stdin"));
list($a, $b) = explode(\' \', $input);
echo $a + $b;
```

-----------------

Rust

```rust
use std::io;

fn main(){
    let mut input=String::new();
    io::stdin().read_line(&mut input).unwrap();
    let mut s=input.trim().split(\' \');

    let a:i32=s.next().unwrap()
               .parse().unwrap();
    let b:i32=s.next().unwrap()
               .parse().unwrap();
    println!("{}",a+b);
}
```

-----------------

Go

```go
package main

import "fmt"

func main() {
    var a, b int
    fmt.Scanf("%d%d", &a, &b)
    fmt.Println(a+b)
}
```

-----------------

C# Mono

```cs
using System;

public class APlusB{
    private static void Main(){
        string[] input = Console.ReadLine().Split(\' \');
        Console.WriteLine(int.Parse(input[0]) + int.Parse(input[1]));
    }
}
```

------------------

Kotlin

```kotlin
fun main(args: Array<String>) {
    val (a, b) = readLine()!!.split(\' \').map(String::toInt)
    println(a + b)
}
```

------------------

Haskell

```haskell
main = do
    [a, b] <- (map read . words) `fmap` getLine
    print (a+b)
```

------------------

Lua

```lua
a = io.read(\'*n\')
b = io.read(\'*n\')
print(a + b)
```

------------------

OCaml

```ocaml
Scanf.scanf "%i %i\\n" (fun a b -> print_int (a + b))
```

------------------

Julia

```julia
nums = map(x -> parse(Int, x), split(readline(), " "))
println(nums[1] + nums[2])
```

------------------

Scala

```scala
object Main {
  def main(args: Array[String]): Unit = {
    import java.util.Scanner

    val cin = new Scanner(System.in)
    val a = cin.nextInt()
    val b = cin.nextInt()
    System.out.println(a + b)
  }
}
```

------------------

Perl

```perl
my $in = <STDIN>;
chomp $in;
$in = [split /[\\s,]+/, $in];
my $c = $in->[0] + $in->[1];
print "$c\\n";
```', 1, 5, 0, '{"timeLimit":1000,"memoryLimit":128}', 2002744707925106690, '2025-12-21 14:18:57', '2025-12-27 02:39:08', 0, 1);

-- 提交表
create table submission
(
    id                 bigint                             not null comment '提交记录ID（雪花算法生成）'
        primary key,
    user_id            bigint                             not null comment '提交用户ID',
    problem_id         bigint                             not null comment '题目ID',
    source_code        text                               not null comment '提交的源代码',
    submission_result  text                               null comment '提交结果（JSON字符串）',
    status             int                                null comment '评测状态码（如0-待评测、1-通过等）',
    status_description varchar(255)                       null comment '评测状态描述（如"Accepted"、"Wrong Answer"）',
    language           varchar(50)                        not null comment '编程语言（如java、python、c++等）',
    create_time        datetime default (now())           not null comment '创建时间（提交时间）',
    update_time        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间（最后评测时间）',
    is_deleted         int      default 0                 not null comment '逻辑删除标志（0-未删除，1-已删除）',
    KEY `idx_user_id` (`user_id`) COMMENT '按用户ID查询提交记录',
    KEY `idx_problem_id` (`problem_id`) COMMENT '按题目ID查询提交记录'
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码提交记录表';
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
INSERT INTO db_oj.tag (id, name, color) VALUES (1, ' 二分', '#FFB6C1');
INSERT INTO db_oj.tag (id, name, color) VALUES (2, 'DFS', '#800080');
INSERT INTO db_oj.tag (id, name, color) VALUES (4, 'BFS', '#4B0082');
INSERT INTO db_oj.tag (id, name, color) VALUES (5, '链表', '#B0C4DE');
INSERT INTO db_oj.tag (id, name, color) VALUES (6, '双指针', '#00BFFF');
INSERT INTO db_oj.tag (id, name, color) VALUES (7, '贪心', '#008080');
INSERT INTO db_oj.tag (id, name, color) VALUES (8, '动态规划', '#D2691E');
INSERT INTO db_oj.tag (id, name, color) VALUES (9, '搜索', '#DAA520');

-- 题目标签和题目的中间表
CREATE TABLE `problem_tag` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键自增',
    `problem_id` bigint NOT NULL COMMENT '题目ID（关联problem表id）',
    `tag_id` int NOT NULL COMMENT '标签ID（关联tag表id）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_problem_tag` (`problem_id`, `tag_id`) COMMENT '唯一约束：避免同一题目重复关联同一标签',
    CONSTRAINT `fk_problem_tag_problem` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_problem_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目-标签关联表（实现多对多关系）';
INSERT INTO db_oj.problem_tag (id, problem_id, tag_id, create_time) VALUES (1, 1, 1, '2025-12-21 14:18:57');
