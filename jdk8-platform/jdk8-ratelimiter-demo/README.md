# jdk8-ratelimiter-demo

> 多租户分布式限流三实现对比：**Redis+Lua（基准，全局精确）→ 本地+Redis（优化，部分精度换高性能）**。SpringBoot 整合，REST 演示 + 属性驱动压测。对应性能优化主题 #01。

## 一句话定位

学一个高维取舍：怎么用「窗口级精度损失」换「热路径零网络开销」，让分布式限流从"每请求打 Redis"变成"本地拦截 + 周期校准"，并把精度损失**量化**出来。

## 技术栈与入口类、触发方式

- 技术栈：Java 8、Spring Boot 2.7.18、Spring Data Redis（Lettuce）、JUnit 5
- 启动类：`lan.chaos.ratelimiter.RatelimiterApplication`
  - 默认：REST 服务（`/api/ratelimiter/*`，端口 8082）
  - 压测：启动追加 `--ratelimiter.bench.enabled=true`，跑完打印并退出
- 三种限流实现（根包 `lan.chaos.ratelimiter`）：
  - `redis.RedisLuaRateLimiter` — 基准：每请求一次 Redis+Lua 令牌桶（`StringRedisTemplate` + `DefaultRedisScript`）
  - `local.LocalRedisRateLimiter` — 优化：本地令牌桶 + 每窗口 Redis 校准
  - `local.LocalOnlyRateLimiter` — 参考：纯本地，性能下界

## 快速开始

```bash
# 1) 起 Redis（如本机已有可跳过）
docker compose up -d

# 2) 构建（产出 SpringBoot 可执行 jar）
mvn -q clean package

# 3) 单元测试（无 Redis 用例直接跑；需 Redis 用例自动跳过）
mvn test

# 4) REST 演示：启动服务
java -jar target/jdk8-ratelimiter-demo-1.0-SNAPSHOT.jar
#    再开一个终端验证放行:
curl "http://localhost:8082/api/ratelimiter/allow?tenant=demo"
curl "http://localhost:8082/api/ratelimiter/stat?tenant=demo&mode=local-redis"

# 5) 压测：未超限对比延迟与 Redis 负载（qps 500 < limit 1000）
java -jar target/jdk8-ratelimiter-demo-1.0-SNAPSHOT.jar --ratelimiter.bench.enabled=true --ratelimiter.bench.mode=redis-lua    --ratelimiter.bench.threads=8 --ratelimiter.bench.qps=500 --ratelimiter.bench.limit=1000 --ratelimiter.bench.duration-sec=10 
java -jar target/jdk8-ratelimiter-demo-1.0-SNAPSHOT.jar --ratelimiter.bench.enabled=true --ratelimiter.bench.mode=local-redis --ratelimiter.bench.threads=8 --ratelimiter.bench.qps=500 --ratelimiter.bench.limit=1000 --ratelimiter.bench.duration-sec=10 --ratelimiter.bench.nodes=4 --ratelimiter.bench.burst-multiplier=1.5

# 6) 压测：超限看精度损失（qps 2000 > limit 1000）
java -jar target/jdk8-ratelimiter-demo-1.0-SNAPSHOT.jar --ratelimiter.bench.enabled=true --ratelimiter.bench.mode=redis-lua    --ratelimiter.bench.threads=8 --ratelimiter.bench.qps=2000 --ratelimiter.bench.limit=1000 --ratelimiter.bench.duration-sec=20
java -jar target/jdk8-ratelimiter-demo-1.0-SNAPSHOT.jar --ratelimiter.bench.enabled=true --ratelimiter.bench.mode=local-redis --ratelimiter.bench.threads=8 --ratelimiter.bench.qps=2000 --ratelimiter.bench.limit=1000 --ratelimiter.bench.duration-sec=20 --ratelimiter.bench.nodes=4 --ratelimiter.bench.burst-multiplier=1.5

# 7) 吞吐上限（flood，不看 qps 满速打）
java -jar target/jdk8-ratelimiter-demo-1.0-SNAPSHOT.jar --ratelimiter.bench.enabled=true --ratelimiter.bench.mode=local-redis --ratelimiter.bench.threads=8 --ratelimiter.bench.limit=1000 --ratelimiter.bench.duration-sec=10 --ratelimiter.bench.nodes=4 --ratelimiter.bench.burst-multiplier=1.5 --ratelimiter.bench.flood=true
```

**本地连接真实环境**：用 `application-local.yml`（已被 gitignore，不提交）覆盖 Redis 端口/密码，`--spring.profiles.active=local`。

预期：`local-redis` 的 `redis/s` 从"≈请求数/秒"降到"每窗口×租户×节点"，`flood` 下吞吐明显高于 `redis-lua`。

## 场景一览

| 场景 | 触发方式 | 一句话解释 |
|---|---|---|
| 令牌桶语义 | `LocalBucketTest` | 满容量一次取足、速率补发、校准收缩丢弃余量 |
| 纯本地限流 | `LocalOnlyRateLimiterTest` | 单实例限流生效，强调多实例会成倍超限 |
| 本地+Redis 集成 | `LocalRedisRateLimiterTest`（需 Redis，无则跳过） | 校准后窗口内收敛于全局限额，burst=1 不超限 |
| REST 放行演示 | `/api/ratelimiter/allow?tenant=` | 按当前 mode 对租户放行并返回指标 |
| REST 指标观察 | `/api/ratelimiter/stat?mode=` | 观察 redis/s、本地命中率 |
| 延迟/吞吐对比 | `--ratelimiter.bench.*=...` | redis-lua vs local-redis 的 avg / p99 / redis/s / overLimit% |
| burst 敏感性 | `burst-multiplier=1.0/1.5/3.0` | burst 越大突发容忍越高、超限上界越高 |
| 流量倾斜 | `skew=0.8` | 大量流量打到单节点，观察欠用/超限 |

## 场景详解

### 1. 设计：本地令牌桶 + Redis 周期校准

- 第一层（本地）：每节点每租户一个内存令牌桶 `LocalBucket`，热路径零网络。
- 第二层（Redis）：全局窗口计数器为权威，每 `windowMs` 每租户每节点校准一次（`StringRedisTemplate#execute` 执行 Lua）。
- 校准 Lua：滚动窗口计数 → `count += 本节点消耗` → `remaining = windowQuota - count` → 返回 `remaining / N` → 本地桶重置 `rate=allocated/windowMs×1000, capacity=allocated×burst`。
- 关键 API：`RateLimiter#tryAcquire(tenantId)` 统一入口；各实现暴露 `redisCalls()` / `localAllows()` 供指标统计。

### 2. 精度损失上界（可解释）

- 本地容量 = 每节点窗口配额 × burst，其中每节点窗口配额 = `windowQuota / N`。
- 最坏：N 节点同时打满本地桶，全局超限 ≈ `(burst-1) × windowQuota`。
- `burst=1`：理论上界 = 全局每窗口配额，但流量倾斜时单节点配额不足 → **欠用**。
- 实测 `local-only` 多节点会约按节点数成倍超限 —— 正是它不能做分布式限流的原因。

### 3. redis-lua 基准的代价

每请求 1 次 `EVAL`，单机高 QPS 下 Redis CPU 与连接成为瓶颈；限流本应是最廉价的守卫，却成了热点依赖。这正是优化动机。

## 配置说明

- `application.yml`：默认配置（`server.port`、`spring.redis.*`、`ratelimiter.*`、`ratelimiter.bench.*`），敏感值用 `${VAR:默认}` 占位。
- `application-local.yml`：真实环境连接凭证（Redis 端口/密码），已被 `.gitignore` 忽略，不会提交；本地运行为 `--spring.profiles.active=local`。

## 进阶方向

- **严格模式**：本地桶打满后回落 Redis 再判一次，精度近乎 100%，边界请求仍打 Redis（吞吐略降）。
- **动态节点发现**：`nodeCount` 固定值 → 从注册中心获取存活节点实时更新。
- **配额分配策略**：均分 N 改为按历史负载加权分配，缓解倾斜欠用。
- **多租户差异化**：不同租户不同 burst / window，支持配置下发。

## 设计要点

- 用**三实现对照**讲清"纯本地（最只管、不跨节）→ Redis+Lua（最精确、最贵）→ 本地+Redis（折中）"，取舍可量化。
- 压测指标刻意给全了延迟、Redis 负载、超限率三者，因为它要回答的不是"快不快"而是"用多少精度换来的快"。
- 压测由属性 `ratelimiter.bench.*` 驱动，一条命令直达，跑完即退出，适合一键采集数据。
- 单元测试分两类：无依赖用例直接跑（保语义），需 Redis 用例 `Assumptions` 跳过（保 CI）。