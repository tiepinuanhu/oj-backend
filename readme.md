# oj-backend

在线评测系统（Online Judge）后端服务。基于 Spring Boot 3 提供题目、提交、比赛、用户等 REST API，并通过 RabbitMQ 异步评测，调用 [go-judge](https://github.com/criyle/go-judge) 沙箱执行用户代码。

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 3、Java 17 |
| ORM | MyBatis-Plus、Druid |
| 缓存 / 锁 | Redis、Redisson |
| 消息队列 | RabbitMQ |
| 全文检索 | Elasticsearch |
| 鉴权 | JWT |
| 远程调用 | OpenFeign（对接 go-judge） |
| 文档 | SpringDoc OpenAPI |
| 容器 | Docker Compose（MySQL / Redis / RabbitMQ） |

## 功能概览

- **用户**：注册、登录、登出、资料与头像、角色权限
- **题目**：增删改查、分页列表、标签、公开/私有；Elasticsearch 全文检索（标题 + 题面）；管理员上传 C++ 标程，按 `.in` / zip 自动生成 `.out` 样例
- **提交**：代码提交、异步判题、提交列表与详情、题目统计、日榜
- **比赛**：创建/更新、报名、赛题管理、比赛提交与榜单
- **判题**：支持 C++ / Java / Python；普通题与比赛题策略分离；沙箱隔离执行

## 系统架构

```text
客户端 ──► oj-backend (/api)
              │
              ├── MySQL           业务数据
              ├── Redis           缓存 / 分布式锁
              ├── Elasticsearch   题目全文检索
              ├── RabbitMQ        提交异步评测
              └── go-judge        编译与运行沙箱 (:5050)
```

判题流程：用户提交 → 写入提交记录 → 投递 MQ → 消费者拉取 → 策略判题（编译 / 逐用例运行）→ 回写结果。

题目样例生成：管理员上传 C++ 标程 → 沙箱编译校验并写入 `problem.standard_code` → 上传 `.in`（或 zip）→ 用标程跑沙箱生成 `.out` 并落盘。

## 环境要求

- JDK 17+
- Maven 3.8+（或使用项目自带 `mvnw`）
- Docker / Docker Compose（推荐用于中间件）
- Elasticsearch 8.x（题目关键词检索）
- go-judge 评测沙箱（可用仓库内 `Dockerfile` 构建）

## 快速开始

### 1. 启动中间件

```bash
docker compose up -d
```

将拉起：

| 服务 | 端口 |
|------|------|
| MySQL 8.0 | 3306 |
| Redis 7.2 | 6379 |
| RabbitMQ（含管理台） | 5672 / 15672 |

另需自行部署 Elasticsearch（默认 `http://{remote.address}:9200`），并保证与 `application.yaml` 一致。

按需修改 `compose.yaml` 中的账号密码，并与 `application.yaml` 保持一致。

### 2. 初始化数据库

**新库**：执行 `sql/init.sql`，创建库 `db_oj`、表结构及示例数据：

```bash
mysql -h 127.0.0.1 -u root -p < sql/init.sql
```

**已有库升级**：若缺少 `problem.standard_code` 列，再执行：

```bash
mysql -h 127.0.0.1 -u root -p db_oj < sql/add_problem_standard_code.sql
```

### 3. 启动评测沙箱（go-judge）

```bash
docker build -t oj-go-judge .
docker run -d --name go-judge -p 5050:5050 oj-go-judge
```

沙箱地址由配置项 `remote.address` 决定，Feign 请求 `http://{remote.address}:5050`。

### 4. 修改配置

编辑 `src/main/resources/application.yaml`。MySQL / Redis / RabbitMQ / ES 等可统一走 `remote.address`：

```yaml
remote:
  address: 127.0.0.1          # 中间件与 go-judge 所在主机

spring:
  servlet:
    multipart:
      max-file-size: 20MB     # 样例 zip / 多文件上传
      max-request-size: 50MB
  datasource:
    druid:
      url: jdbc:mysql://${remote.address}:3306/db_oj
      username: root
      password: <your_password>
  data:
    redis:
      host: ${remote.address}
      password: <your_password>   # 无密码可留空并调整配置
  rabbitmq:
    addresses: ${remote.address}
    username: admin
    password: <your_password>
  elasticsearch:
    uris: http://${remote.address}:9200

oj:
  data:
    path: /path/to/oj-data      # 题目测试数据等本地目录
```

服务默认端口：`8080`，上下文路径：`/api`。

### 5. 启动应用

```bash
./mvnw spring-boot:run
# Windows
mvnw.cmd spring-boot:run
```

或：

```bash
./mvnw -DskipTests package
java -jar target/oj-backend-0.0.1-SNAPSHOT.jar
```

启动后访问：

- API 根路径：`http://localhost:8080/api`
- Swagger UI：`http://localhost:8080/api/swagger-ui.html`（若已启用 SpringDoc）

首次启动后题目会同步到 Elasticsearch；增删改题目时也会更新 ES 与相关缓存。

## 主要 API 模块

| 前缀 | 说明 |
|------|------|
| `/api/user/**` | 用户注册登录、资料管理 |
| `/api/problem/**` | 题目 CRUD、列表、ES 检索、标程与样例生成 |
| `/api/submission/**` | 提交、列表、详情、统计、日榜 |
| `/api/contest/**` | 比赛、报名、赛题、比赛提交与榜单 |
| `/api/tag/**` | 题目标签 |

### 题目相关补充接口（管理员）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/problem/search` | Elasticsearch 全文检索（标题 + 题面） |
| POST | `/api/problem/uploadStandard` | 上传并校验 C++ 标程，写入 `standard_code` |
| GET | `/api/problem/getStandard` | 查询题目标程（样例管理回填） |
| POST | `/api/problem/generateCases` | 按请求体中的输入用标程生成样例 |
| POST | `/api/problem/generateCasesByFiles` | 上传 `.in` / zip，用标程生成并覆盖样例 |

## 项目结构

```text
oj-backend/
├── compose.yaml              # MySQL / Redis / RabbitMQ
├── Dockerfile                # go-judge 镜像
├── sql/
│   ├── init.sql              # 库表初始化（含 standard_code）
│   └── add_problem_standard_code.sql  # 已有库升级脚本
└── src/main/java/com/wxc/oj/
    ├── controller/           # REST 接口
    ├── service/              # 业务逻辑（含 ProblemCase / ProblemEs）
    ├── judger/               # 判题策略、SandboxCodeRunner
    ├── es/                   # Elasticsearch Repository
    ├── openFeign/            # go-judge / 图片等远程调用
    ├── mapper/               # MyBatis Mapper
    ├── model/                # 实体 / DTO / VO
    ├── config/               # 配置类
    ├── interceptor/          # 拦截器（鉴权等）
    └── cache/                # 缓存相关
```

## 判题说明

- 语言配置见 `oj.language`（源文件名、编译命令、运行命令）
- 默认资源限制见 `oj.judge`（CPU / 内存 / 进程数等）
- 测试数据路径依赖 `oj.data.path`，部署前请创建并保证进程可读写
- 标程目前仅支持 C++；生成样例依赖 go-judge 可用

## 与前端联调

前端项目 [oj-frontend](https://github.com/tiepinuanhu/oj-frontend) 默认请求：

```text
http://localhost:8080/api
```

确保后端已启动且 CORS / 鉴权配置允许本地前端访问。

## License

Private / 学习项目，按需自行约定使用范围。
