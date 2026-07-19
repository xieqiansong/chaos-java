# jdk8-redis-demo

Redis（内存键值数据库）演示模块，覆盖**字符串/对象缓存、Hash/List/Set、ZSet 排行榜、计数自增、限流、分布式锁、Lua、Pipeline、发布订阅**等核心能力（基于 Spring Data Redis + Lettuce）。单模块多包，**以单元测试为核心验证手段**，无 Web 层。

- 基础包：`lan.chaos.redis`
- 技术栈：Spring Boot 2.7.18 + Spring Data Redis 2.7.x + Lettuce（默认客户端，走 `6379`）
- 验证入口：`src/test/.../RedisScenarioTest`（覆盖缓存 JSON 往返 / 排行榜 TopN / 计数原子 / 分布式锁），无 Redis 时用例自动跳过

> 所有场景均连真实 Redis（`localhost:6379`）。跑测试前先启动 Redis Server 即可；不启动也能 `mvn test`（集成用例跳过，上下文装配用例照常通过）。

> 使用频率标注：`★★★ 高频`（几乎每个项目都会用）／`★★☆ 中频`（常见但不一定都有）／`◆ 基础`（连接/配置模块，非独立业务场景）。

## 目录结构

```
jdk8-redis-demo/
├── pom.xml                                  # 继承 jdk8-platform，Spring Boot + spring-boot-starter-data-redis + commons-pool2
├── src/main/resources/application.yml       # Redis 连接与 Lettuce 连接池配置 ◆
    └── src/main/java/lan/chaos/redis
    ├── RedisApplication.java                # 启动类
    ├── common/                              # 支撑（触发/配置/模型，边缘关注，非场景）
    │   ├── config/
    │   │   ├── RedisConfig.java             # RedisTemplate(JSON)/StringRedisTemplate 配置 ◆
    │   │   └── PubSubConfig.java            # 发布订阅监听器容器配置 ★★☆
    │   ├── constant/
    │   │   └── RedisKeyConstants.java       # Key 命名规范（前缀/频道）
    │   ├── model/
    │   │   └── User.java                    # 演示实体（对象缓存）
    ├── cache/                               # 字符串/对象缓存 ★★★
    │   └── StringCacheService.java
    ├── collection/                          # Hash/List/Set ★★★
    │   └── CollectionService.java
    ├── rank/                                # ZSet 排行榜 ★★★
    │   └── RankService.java
    ├── counter/                             # 计数/自增(INCR) ★★★
    │   └── CounterService.java
    ├── lock/                                # 分布式锁(SET NX EX + Lua) ★★☆
    │   └── DistributedLock.java
    ├── ratelimit/                           # 限流(Lua) ★★☆
    │   └── RateLimitService.java
    ├── stock/                               # Lua 扣库存 ★★☆
    │   └── StockService.java
    ├── pipeline/                            # Pipeline 批量 ★★☆
    │   └── PipelineService.java
    └── pubsub/                              # 发布订阅 ★★☆
        └── PubSubService.java
```

> 设计要点：**能力场景是顶层包**（`cache/collection/rank/...`），`config/constant/model` 这类「配置与支撑」只是学习边缘关注，统一收进 `common/`（与 `jdk8-rocketmq-demo` 的能力顶层包 + `common/` 公共能力一致）。

## 场景一览（按使用频率排序）

`★★★ 高频`
- [字符串/对象缓存 cache](#1-字符串对象缓存-cache) → `SET/GET/EXPIRE`、对象 JSON 缓存
- [Hash/List/Set 集合 collection](#2-hashlistset-集合-collection) → 购物车、队列、点赞去重
- [ZSet 排行榜 rank](#3-zset-排行榜-rank) → 积分/热度 TopN、`incrementScore`
- [计数自增 counter](#4-计数自增-counter) → 原子 `INCR`、点赞数/访问量

`★★☆ 中频`
- [分布式锁 lock](#5-分布式锁-lock) → `SET NX EX` + Lua 安全释放
- [限流 limit](#6-限流-limit) → 固定窗口，Lua 保证原子
- [Lua 扣库存 stock](#7-lua-扣库存-stock) → 判断-扣减原子，解决超卖
- [Pipeline pipeline](#8-pipeline-批量-pipeline) → 一次往返批量读写
- [发布订阅 pubsub](#9-发布订阅-pubsub) → 频道消息广播

`◆ 基础模块`
- [RedisConfig 连接配置](#redisconfig-连接配置)

---

### RedisConfig 连接配置 `◆`

Spring Boot 默认 `RedisTemplate` 用 JDK 序列化（二进制难读），这里自定义两个模板：

| 模板 | key 序列化 | value 序列化 | 用途 |
|------|-----------|-------------|------|
| `redisTemplate` (`<String,Object>`) | String | JSON（带 `@class` 类型） | 对象缓存，可正确反序列化回 Java 对象 |
| `stringRedisTemplate` | String | String | 简单字符串、计数、Pipeline、Lua 等 |

连接信息见 `application.yml`：`host/port/password/database` + `lettuce.pool`（连接池）。

---

### 1. 字符串/对象缓存 cache `★★★`

最常用结构：缓存热点数据、对象、分布式会话。重点掌握带过期时间的写缓存与 TTL 查询。

- 写：`set(key, value, ttl, SECONDS)`；读：`get(key)`；删：`delete(key)`；剩余时间：`getExpire(key)`
- 对象缓存：用 JSON 序列化器把 `User` 存入 Redis，value 为可读 JSON

验证：见 `RedisScenarioTest.cacheUser_roundTrip_json`（对象 JSON 往返）。

---

### 2. Hash/List/Set 集合 collection `★★★`

- **Hash**：一个 key 下多个 `field-value`，适合对象属性、购物车（`HSET/HGET/HGETALL`）。
- **List**：有序可重复，左进右出即队列（`LPUSH/RPOP/LRANGE`）。
- **Set**：无序去重，适合点赞、标签、共同好友（`SADD/SISMEMBER/SMEMBERS`）。

验证：直接调用 `CollectionService` 的 `hSet/hGet/lPush/sAdd` 等方法，或补一条测试断言。

---

### 3. ZSet 排行榜 rank `★★★`

每个 member 关联 score，Redis 自动按 score 排序，天然适合排行榜。`REVRANGE` 取 TopN，`INCRBY` 原子增减分数。

- 加/增减：`addScore` / `incrScore`
- 倒序 TopN（带分数）：`rankWithScore(topN)`
- 名次：`rankOf(member)`

验证：见 `RedisScenarioTest.rank_topN_desc`（TopN 倒序）。

---

### 4. 计数自增 counter `★★★`

`INCR` 是原子操作，单线程 Redis 保证并发安全，常用于点赞数、访问量、分布式 ID。

- 自增：`incr(key)`；按步长：`incrBy(key, delta)`；自减：`decr(key)`；读取：`get(key)`

验证：见 `RedisScenarioTest.counter_incr_atomic`（原子自增）。

---

### 5. 分布式锁 lock `★★☆`

基于 `SET key uuid NX EX`：只有 key 不存在才能加锁成功（原子）并带过期防死锁；释放用 Lua 校验 uuid 后删除，避免误删他人锁。

- `tryLock(lockKey, requestId, expireSeconds)` → 加锁
- `release(lockKey, requestId)` → 安全释放（Lua）
- `withLock(lockKey, expire, action)` → 模板式用法（推荐）

验证：见 `RedisScenarioTest.lock_runsExactlyOnce`（加锁→业务→释放）。

---

### 6. 限流 limit `★★☆`

固定窗口限流：用 Lua 脚本保证「计数 + 设过期」原子，避免并发竞态。`KEYS[1]` 为限流 key，`ARGV[1]` 窗口秒数，`ARGV[2]` 最大次数；返回 1 放行 / 0 拦截。

验证：直接调用 `RateLimitService.tryAcquire(key, window, max)`，连续调用观察 `allow`/`blocked`。

---

### 7. Lua 扣库存 stock `★★☆`

扣减库存用 Lua 脚本原子执行「读-判-扣」，解决并发超卖。返回 1 成功 / 0 库存不足 / -1 未初始化。

验证：先 `init` 再并发调 `deduct`，观察返回值 `1 成功 / 0 售罄 / -1 未初始化`（不会超卖）。

---

### 8. Pipeline 批量 pipeline `★★☆`

一次网络往返批量发送多条命令，大幅提升写入吞吐（对比逐条 `SET` 的多次往返）。

验证：调用 `PipelineService.demo()` 批量写入 5 条并返回读回结果。

---

### 9. 发布订阅 pubsub `★★☆`

向频道发布消息，订阅方（见 `common/config/PubSubConfig`）收到后打印日志。适合轻量广播、事件通知。

验证：调用 `PubSubService.publish(msg)`，观察应用控制台输出 `[Redis Pub/Sub] ...`。

---

## 如何运行

```bash
# 1)（可选）启动 Redis（默认 6379，学习用单实例即可）
docker run -d --name redis -p 6379:6379 redis:7.2

# 2) 跑测试：核心场景由 RedisScenarioTest 验证
mvn -pl jdk8-redis-demo -am test
```

- 有本地 Redis：4 条集成用例（缓存 JSON 往返 / 排行榜 TopN / 计数原子 / 分布式锁）真实执行并通过。
- 无 Redis：集成用例经 `Assumptions` 自动跳过，`RedisApplicationTests`（上下文装配）照常通过，CI 也能绿。

也可单独跑某个场景：在 IDE 里直接执行 `RedisScenarioTest` 的对应方法，或写一条新测试调用任意 Service（如 `CollectionService`、`RateLimitService`）观察输入→输出。

## 进阶方向（依赖外部组件 / 生产考量，未写成独立 Demo）

- `◆` **Redis 集群 / 哨兵**：高可用与分片（配置 `spring.redis.cluster` / `spring.redis.sentinel`）
- `◆` **Redisson 分布式锁**：成熟锁实现（可重入、看门狗续期、红锁），生产推荐替代手写锁
- `◆` **缓存三大问题**：缓存穿透（布隆过滤器/空值缓存）、击穿（互斥锁/逻辑过期）、雪崩（随机 TTL/多级缓存）
- `◆` **Stream**：Redis 5+ 的流结构，可做消息队列（替代 Pub/Sub 的无持久化短板）
- `◆` **持久化与内存**：RDB/AOF 策略、maxmemory 与淘汰策略（allkeys-lru 等）

## 设计要点

- **单模块多包**：缓存、集合、排行榜、计数、锁、进阶各成包，贴近真实 Redis 服务结构。
- **序列化选择**：对象用 JSON（可读、跨语言），简单值用 String 模板，避免默认 JDK 二进制。
- **原子性优先**：计数 `INCR`、扣库存/限流用 Lua，避免「读-改-写」并发竞态。
- **锁要安全释放**：分布式锁释放必须校验持有者，用 Lua 原子删除，杜绝误删。
- **频率结论**：生产里 **字符串缓存 + Hash/List/Set + 计数 + ZSet 排行榜** 几乎必写；**分布式锁、限流、Lua、Pipeline、Pub/Sub** 按业务选用；**集群、Redisson、缓存三大问题、Stream** 属进阶/生产范畴。
