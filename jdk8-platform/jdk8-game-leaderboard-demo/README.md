# jdk8-game-leaderboard-demo

游戏**实时积分排行榜**演示模块，对应牛客面经系统设计题「如何设计一个游戏实时积分排行榜，支持百万用户实时更新和高效查询（如 Top 100）？」的完整落地。

以 **Redis ZSET（跳跃表 + 哈希表）** 为内核，叠加四层优化：**双层缓存（Top100）/ 热数据双写（Top1000）/ 分片聚合（跨分片 Top K）/ 异步更新流水线**。通过 `ScoreBoard` 存储抽象，全部逻辑可在内存下被完整测试；切到 `redis` 存储即走真实 ZSET。

- 基础包：`lan.chaos.leaderboard`
- 技术栈：Spring Boot 2.7.18 + Spring Data Redis(Lettuce) + Caffeine 2.9.3（JDK8 用 2.x）
- 验证入口：
  - 控制台：`DemoApp.main`（5 个场景分节打印「输入→输出」，默认 memory 存储，**零外部依赖**直接跑）
  - 测试：`LeaderboardScenarioTest`（内存逻辑全量断言）/ `LeaderboardRedisTest`（Redis 集成，`Assumptions` 守卫，无 Redis 自动跳过）

> 频率标注：`★★★ 高频`／`★★☆ 中频`／`◆ 基础`（连接/配置模块，非独立业务场景）。

## 目录结构

```
jdk8-game-leaderboard-demo/
├── pom.xml                                  # 继承 jdk8-platform：data-redis + caffeine + commons-pool2
├── docker-compose.yml                       # 本地 Redis（store=redis 时启用）
├── src/main/resources/application.yml       # leaderboard.store=memory（默认）/ redis 等参数 ◆
└── src/main/java/lan/chaos/leaderboard
    ├── LeaderboardApplication.java          # 启动类
    ├── DemoApp.java                         # 控制台 Runner：分节打印各场景「输入→输出」
    ├── common/                              # 支撑（抽象/配置/常量/模型，边缘关注）
    │   ├── config/CacheConfig.java          # 本地 Top100 缓存（Caffeine）◆
    │   ├── constant/LeaderboardConstants.java # key/容量常量 ◆
    │   ├── model/RankEntry.java             # 排行条目（member/score/rank）
    │   └── store/
    │       ├── ScoreBoard.java              # ★ 存储抽象接口（解耦底层）
    │       ├── MemoryScoreBoard.java        # 纯内存实现（测试/零依赖演示）
    │       ├── RedisScoreBoard.java         # Redis ZSET 实现（生产后端）
    │       └── ScoreBoardFactory.java       # 按 leaderboard.store 选后端
    ├── core/                                # ① 核心 ZSET 排行榜 ★★★
    │   └── ZsetLeaderboardService.java
    ├── doublecache/                         # ② 双层缓存 Top100 ★★☆
    │   └── TopBoardCacheService.java
    ├── hot/                                 # ③ 热数据双写（Top1000）★★☆
    │   └── HotDataLeaderboardService.java
    ├── shard/                               # ④ 分片 + 跨分片聚合 TopK ★★☆
    │   └── ShardedLeaderboardService.java
    └── pipeline/                            # ⑤ 异步更新流水线 ★★☆
        └── AsyncScorePipeline.java
```

> 设计要点：**能力场景是顶层包**（`core/doublecache/hot/shard/pipeline`），一一对应面经方案的五节；`config/constant/model/store` 这类「抽象与支撑」统一收进 `common/`。`ScoreBoard` 抽象是让整套逻辑可零依赖测试的关键。

## 场景一览（对应面经五节）

`★★★ 高频`
- [核心 ZSET 排行榜 core](#1-核心-zset-排行榜-core) → 更新 O(logN) / TopK 查询 O(logN+K) / 查自己附近名次

`★★☆ 中频`
- [双层缓存 Top100 doublecache](#2-双层缓存-top100-doublecache) → 本地 Caffeine 缓存 ZSET 算出的 Top100，命中即返回
- [热数据双写 hot](#3-热数据双写-hot) → 全量板 + Top1000 专用小板双写，查询只走小板
- [分片聚合 shard](#4-分片聚合-shard) → 哈希分片打散，跨分片归并出全局 TopK
- [异步更新流水线 pipeline](#5-异步更新流水线-pipeline) → 高频更新攒批落盘，削峰降存储压力

`◆ 基础模块`
- [ScoreBoard 存储抽象](#scoreboard-存储抽象)
- [CacheConfig 本地缓存](#cacheconfig-本地缓存)

---

### ScoreBoard 存储抽象 `◆`

把「底层存储」从业务逻辑中解耦：

| 实现 | 用途 | 复杂度 |
|------|------|--------|
| `MemoryScoreBoard` | 单元测试 + 零依赖演示（`store=memory`，默认） | 读 O(N log N)，仅演示 |
| `RedisScoreBoard` | 生产真实后端（`store=redis`）：`ZADD/ZINCRBY` 更新、`ZREVRANGE WITHSCORES` 查 TopK | 更新 O(logN)、查 TopK O(logN+K) |

语义对齐 Redis ZSET：score 为 double、变化用 `incrementScore` 原子累加；`topDesc` 倒序（rank 0 基）；同分按 member 字典序小者靠前，保证 Top K 结果可复现。

---

### CacheConfig 本地缓存 `◆`

为「双层缓存」场景提供 Caffeine 容器（`expireAfterWrite=300s` + 业务侧 `refresh()`），缓存由 `ScoreBoard` 算出的 Top100，避免每次请求打 Redis。与面经伪代码「定时刷新 Top100 缓存」一致。

---

### 1. 核心 ZSET 排行榜 core `★★★`

面经「核心数据结构选择」：Redis ZSET 天然适合积分排行，更新与 TopK 均为对数级。

- 更新：`updateScore(userId, delta)`（`ZINCRBY`，原子累加）
- Top K：`topN(n)`（`ZREVRANGE`，高分在前）
- 名次：`rankOf(userId)`（`ZREVRANK`，0 基）
- 附近名次：`rankAround(userId, radius)`（百万用户查自己排名时，只取小窗口，避免全量拉取）

验证：见 `LeaderboardScenarioTest.core_topN_rank_around`（TopN / 名次 / 附近名次均正确）。

---

### 2. 双层缓存 Top100 doublecache `★★☆`

面经「Top K 查询优化 - 双层缓存策略」：本地 Caffeine 缓存「由 ZSET 算出的 Top100」。

- `getTop()`：命中本地缓存即返回（亚毫秒），未命中回源 `ScoreBoard` 并写入缓存
- `refresh()`：主动失效 + 重拉（对应文档 `refresh_top100` 定时刷新）
- 这是「本地缓存(Redis 之上) + 分布式存储」的双层结构：本地挡热点、Redis 挡共享

验证：见 `LeaderboardScenarioTest.doubleCache_staleUntilRefresh`（更新后缓存未刷新前仍返回旧 Top，refresh 后立即反映最新）。

---

### 3. 热数据双写 hot `★★☆`

面经「热数据特殊处理」：同时维护全量板与 Top1000 专用小板，更新时**双写**，查询只走更小的热数据板。

- `updateScore`：全量板 + 热数据板都更新
- `topFromHot(k)`：只走热数据板（更小、更快）
- `trimHot()`：周期性把热数据板裁剪回 Top N，避免无限膨胀

验证：见 `LeaderboardScenarioTest.hot_dualWriteAndTrim`（双写一致；trim 后收敛到 Top1000）。

---

### 4. 分片聚合 shard `★★☆`

面经「集群化扩展（数据分片）」+「跨分片 Top K 聚合」：按 `hash(userId) % N` 打散到多个 ZSET 分片，单分片容量下降、写入可水平扩展；查询全局 Top K 时各分片取本地 Top K 后合并归并。

- `updateScore`：路由到对应分片
- `globalTopK(k)`：各分片本地 TopK → 合并排序取全局 TopK
- `globalRank(userId)`：统计所有分片中排名高于它的成员数（跨分片名次）

验证：见 `LeaderboardScenarioTest.shard_globalTopK_and_rank`（全局 TopK 有序、u1000 名次为 #1）。

---

### 5. 异步更新流水线 pipeline `★★☆`

面经「扩展方案 - 异步更新流水线（Kafka 解耦）」：高频积分更新先入内存有界队列，后台线程**攒批**后批量写入 `ScoreBoard`，把「多次随机写」合并为「少量批量写」，削峰降存储压力（生产用 Kafka 做同样解耦）。

- `submit(userId, delta)`：非阻塞投递
- 后台 `loop()`：攒满一批或超时即批量落盘
- `awaitQuiescent(timeout)`：等待清空（测试断言用）
- `shutdown()`：`DisposableBean` 容器关闭时自动停后台线程

验证：见 `LeaderboardScenarioTest.pipeline_asyncFlush_reflectsSum`（2000 次更新最终积分与 flushed 计数一致）。

---

## 如何运行

```bash
# 1) 单元测试（核心验证，默认 memory 存储，纯内存零外部依赖，任何环境直接过）
mvn -pl jdk8-game-leaderboard-demo -am test

# 2) 看控制台「输入→输出」：直接运行 DemoApp.main（默认 memory，无需任何中间件）
mvn -pl jdk8-game-leaderboard-demo -am spring-boot:run
#   或 IDE 里直接执行 DemoApp.main

# 3) 切真实 Redis ZSET（需本地 Redis）
docker compose -f jdk8-game-leaderboard-demo/docker-compose.yml up -d
mvn -pl jdk8-game-leaderboard-demo -am test -Dspring.profiles.active=redis
```

预期（DemoApp 控制台节选）：

```
========== 1. 核心 ZSET 排行榜 core ==========
updateScore(u5, +500) -> 1000.0
topN(5): [#1 u10=1000.0, #2 u9=900.0, #3 u5=1000.0? ...]   (u5 因 +500 名次上升)
rankOf(u5) -> #X
rankAround(u5, 1): [...]

========== 2. 双层缓存 Top100 doublecache ==========
首次 getTop() -> #1 u200=200.0 ...
u1 刷成最高分后(未刷新) getTop()[0] -> #1 u200=200.0（仍命中旧缓存）
refresh() 后 getTop()[0] -> #1 u1=999999.0（已是最新）
```

## 进阶方向（生产考量，未写成独立 Demo）

- `◆` **Redis Cluster 分片**：单实例容量不足时走 `spring.redis.cluster`，真分片由集群负责（本 demo 的 `shard` 是应用层分片，用于演示跨分片聚合）
- `◆` **Redisson 分布式集合**：文档提到的 `RLexSortedSet` 跨分片聚合，可用 Redisson 成熟组件替代手写归并
- `◆` **最终一致性**：异步流水线 / 缓存刷新带来的短暂不一致，按业务容忍度取舍
- `◆` **分级存储 / 持久化**：ZSET 之上接 DB 落库做分级存储；RDB/AOF 与 maxmemory 淘汰策略保内存

## 设计要点

- **ZSET 为内核**：更新 O(logN)、TopK 查询 O(logN+K)，百万用户约 50MB、Top100 稳定 2ms 内（与面经一致）。
- **四层优化各司其职**：双层缓存挡读热点、热数据双写缩小查询面、分片水平扩展写、异步流水线削峰。
- **存储抽象是灵魂**：`ScoreBoard` 让上层五个能力包完全不依赖 Redis，既能在内存下零依赖跑通演示与全量单测，又能一行配置切到生产 ZSET。
- **能力即顶层包**：`core/doublecache/hot/shard/pipeline` 各自聚焦面经一节，一个类讲清一个优化点。
