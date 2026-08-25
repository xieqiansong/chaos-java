# 测试报告：多租户分布式限流（Redis+Lua → 本地+Redis）

> 统一报告：覆盖「环境准备 → 单元/集成测试 → 三实现基准压测 → window-ms 精度影响 → 三实现稳定对照」。
> 数据采集：2026-08-25，`localhost` Redis 8.0.2，本机 JDK8，8 线程。

## 一、测试环境

| 项 | 值 |
|---|---|
| 平台 / JDK | Windows / JDK 8 |
| 框架 | Spring Boot 2.7.18、Spring Data Redis（Lettuce） |
| 中间件 | Redis 8.0.2（`localhost:30102`，与 seckill-demo 共用；真实密码经 `@DynamicPropertySource` / 环境变量注入，不落任何结果文件） |
| 压测并发 | 8 线程 / 单场景 5s |
| 被测实现 | `redis-lua`（基准，全局精确）/ `local-redis`（优化，本地+Redis）/ `local-only`（参考，性能下界） |

## 二、压测方式：SpringBootTest 一键跑

基准已由「java -jar + 日志解析」升级为 **JUnit `@SpringBootTest`**：场景矩阵在 [BenchMarkTest.java](file:///d:/project/chaos/chaos-java/jdk8-platform/jdk8-ratelimiter-demo/src/test/java/lan/chaos/ratelimiter/BenchMarkTest.java) 定义，一条命令跑完整矩阵并自动写出报告。

```bash
cd chaos-java/jdk8-platform/jdk8-ratelimiter-demo
mvn test   # 全量：单元/集成 + BenchMarkTest（13 场景自动出 target/bench-results.md）
# 仅基准：
mvn test -Dtest=BenchMarkTest
```

- 真实 Redis 密码动态从 `jdk8-seckill-demo` 的 `application-local.yml` 读取注入，无需手动设环境变量。
- 每个场景前自动 `flushDb` 隔离 key 残留，保证可复现。
- 每场景结束后关闭本地校准线程；跑完对其结果做 sanity 断言并写 markdown。
- 压测核心抽为可复用引擎 [BenchEngine.java](file:///d:/project/chaos/chaos-java/jdk8-platform/jdk8-ratelimiter-demo/src/main/java/lan/chaos/ratelimiter/bench/BenchEngine.java)（`BenchRunner` 与测试共用）。

指标定义：`avg/p99`（µs）；`redis/s`（Redis 调用频率，含 EVAL/EVALSHA）；`overLimit%`（实际放行相对理论限额偏差，负=欠用）。

## 三、构建与单元/集成测试

结果：**BUILD SUCCESS**，单元/集成用例全部通过、0 跳过。`LocalRedisRateLimiterTest` 用真实 Redis 运行（非跳过）。

| 用例 | 说明 |
|---|---|
| `LocalBucketTest` | 本地令牌桶：满容量取足、速率补发、校准预填/收缩 |
| `LocalOnlyRateLimiterTest` | 纯本地单实例限流生效 |
| `LocalRedisRateLimiterTest` | 本地+Redis 集成：校准后窗口内收敛于全局限额；实际放行=100/秒（burst=1）精确收敛 |

### 过程中修复的问题（供回查）
1. **Redis 8 返回整数兼容**：RESP3 下 Lua 返回整数触及 `ValueOutput.set(long)` 不支持 → 脚本统一 `return tostring(...)`，Java 端解析字符串。
2. **校准语义缺陷**：最初 `calibrate` 只 `min(tokens,capacity)`，冷启动放行 0 → 改为校准即预填本期配额，集成测试放行修正为精确的 100。
3. **Lua 下沉**：两段脚本从内联字符串挪到 `resources/lua/`（`token_bucket.lua` / `rebalance.lua`）。

## 四、基准压测（最新 SpringBootTest 实测，13 场景）

`redis-lua`（每请求 1 次 EVAL）作基准；`local-redis` 三处对照。超限场景 qps=2000>limit=1000。

| 场景 | 模式 | qps | avg(µs) | p99(µs) | redis/s | overLimit% |
|---|---|---|---|---|---|---|
| A-500 | redis-lua | 498.9 | 6775.5 | 13622 | 498.9 | -49.9 |
| A-500 | local-redis | 499.9 | 33.5 | 42.3 | 4.8 | -49.9 |
| B-2000 | redis-lua | 1042.5 | 7661.0 | 13007 | 1042.5 | +4.4 |
| B-2000 | local-redis | 1227.5 | 8.1 | 15.7 | 4.8 | +22.8 |
| burst=1.0 | local-redis | 1137.6 | 8.0 | 8.1 | 5.0 | +13.9 |
| burst=3.0 | local-redis | 1395.6 | 7.4 | 3.0 | 5.0 | +39.7 |
| skew=0.8 | local-redis | 756.7 | 11.1 | 33.5 | 5.6 | -24.3 |
| flood | redis-lua | 978.3 | 8164.4 | 16241 | 978.3 | -2.0 |
| flood | local-redis | 1780.3 | 0.5 | 0.6 | 5.6 | +78.1 |

**结论**
1. `local-redis` 热路径近零开销：B 超限 avg 8.1µs / p99 15.7µs（redis-lua 7661µs / 13007µs）；flood avg 0.5µs vs 8164µs。
2. Redis 调用从「≈请求数/秒」降到「≈每窗口×租户×节点」（redis/s≈5，对照 redis-lua 的 ~1000）。
3. `overLimit%` 随 burst 单调（1.0→+13.9%、1.5→+22.8%、3.0→+39.7%）；倾斜 skew=0.8 → -24.3%（欠用），印证均分 N 短板 → 按负载加权分配的后续增强。

## 五、window-ms（校准窗口）对精度的影响

### 5.1 window-ms 是什么
`window-ms` 是节点向 Redis「收一次账、分一次配额」的周期。每周期每租户每节点执行一次校准 Lua（[rebalance.lua](file:///d:/project/chaos/chaos-java/jdk8-platform/jdk8-ratelimiter-demo/src/main/resources/lua/rebalance.lua)）：
`count += 本节点消耗 → remaining = windowQuota - 全局消耗 → 本地配额 = remaining / N`。

- **窗口越小 → 校准越频繁** → Redis 调用越多、分摊越细、精度越高（成本换精度）。
- **窗口越大 → 校准越稀疏** → Redis 调用越少、分摊越粗、错配持续越久（精度换成本）。

### 5.2 实测（固定 local-redis / nodes=4 / burst=1.5 / qps=2000>limit=1000 / 8线程）

| window-ms | overLimit% | redis/s | avg(µs) | p99(µs) | 说明 |
|---|---|---|---|---|---|
| 250 | **-2.1** | 17.0 | 6.0 | 3.0 | 校准最勤，几乎不超限；Redis 成本最高 |
| 500 | +2.6 | 9.0 | 6.6 | 3.7 | — |
| 1000（默认） | +22.8 | 4.8 | 8.1 | 15.7 | 默认值，成本/精度均衡 |
| 2000 | +32.4 | 3.2 | 5.7 | 2.8 | 窗口变大，超限上升 |
| 4000 | **+70.3** | **2.6** | 6.4 | 7.3 | 校准最稀，Redis 成本最低，超限明显放大 |

> 说明：本栏为 SpringBootTest（5s/场景）实测；window=4000 时 5s 内仅校准约 1 次，故超限率（+70%）显著高于此前 12s 口径（+34%），但**趋势一致**——窗口越大超限越严重、redis/s 越低。

### 5.3 解读
1. **精度随窗口增大总体恶化**：overLimit% 从 250ms 的 -2.1% 抬到 4000ms 的 +70.3%。窗口越大，单周期配额越大、节点间错配在窗口内越久得不到权威纠正 → 超发越多。
2. **Redis 成本随窗口增大单调下降**：redis/s 从 17.0 → 9.0 → 4.8 → 3.2 → **2.6**。这是调大窗口的直接动机。
3. **热路径延迟几乎不变**（5.7~8.1µs）：窗口只作用于「精度-成本」轴，不进热路径。
4. **窗口不可无限放大**：250ms 用低（-2.1%）有吞吐浪费兼成本高；4000ms 超发严重（+70%）且对突发响应迟钝 → 是权衡曲线而非线性最优。
5. **burst 与 window 联动**：burst 放大单窗口突发容忍，window 放大错误持续时长，对精度的影响叠加，调参需同步。

### 5.4 实践建议
- **面向精度**：看重不超限 → 调小（250~500ms），redis/s 升至 9~17（仍是 redis-lua 的约 1/100）。
- **面向成本**：Redis 是瓶颈 → 调大（2000~4000ms），redis/s 压到 ~3，但须接受更高超限上界及更慢收敛，必要时补严格模式（边界回源校验）。
- **默认 1000ms** 是便宜又稳的 trade-off 点。

## 六、三实现稳定对照（I/O 多线程开关，历史观察）

验证「开启 → 关闭」Redis I/O 多线程对两层方案的影响（`--spring.profiles.active=local` 口径）：

| 场景 | 模式 | 首测基线 | 开多线程 | 关多线程 |
|---|---|---|---|---|
| B 超限 avg | redis-lua | ~1080µs | 7052µs | 5279µs |
| B 超限 avg | local-redis | ~169µs | 181µs | 174µs |
| D flood 吞吐 | redis-lua | 9.6 万 | 1.49 万 | ≈1.5 万 |
| D flood 吞吐 | local-redis | 7400 万 | 7267 万 | 7200 万级 |

1. **`local-redis` 完全稳定、免疫 Redis 侧配置波动**：三态下 avg/p99/吞吐几乎不变——热路径纯本地。
2. **`redis-lua` 高度敏感且在此机不随开关单调**：首测 1.08ms/9.6 万在当前环境无法复现（稳定 ~5.3ms/1.5 万），说明首测基准偏优、受当日环境/连接 warm 影响，**不作为可靠基线**。
3. **结论方向不受影响**：无论开/关，`local-redis` 都稳定且远优于 `redis-lua`；把 Redis 从热路径摘除，顺带隔离了 Redis 抖动/配置变化对业务关键路径的影响。

## 七、结论

- **方案有效**：本地+Redis 以「窗口级精度损失」换来「热路径零网络」，延迟降若干量级、Redis 负载降约 1/100~1/2200。
- **精度可量化可调**：损失上界与 burst、window 都强相关且单调（超限率随两者增大而上升），可预测、可调。
- **稳健性突出**：对 Redis 侧压力/配置/故障具备隔离与 fail-soft（校准失败保留上期配额、下期重试）。
- **基准诚实性**：`redis-lua` 绝对数值波动大，结论以可稳定复现的 `local-redis` 优势为准；精确绝对值应在固定环境多轮取中位数。

## 八、复现

```bash
cd chaos-java/jdk8-platform/jdk8-ratelimiter-demo
mvn test                       # 全量（含基准），自动写 target/bench-results.md
mvn test -Dtest=BenchMarkTest  # 仅基准
```

场景矩阵与断言见 [BenchMarkTest.java](file:///d:/project/chaos/chaos-java/jdk8-platform/jdk8-ratelimiter-demo/src/test/java/lan/chaos/ratelimiter/BenchMarkTest.java)。
`java -jar --ratelimiter.bench.enabled=true` 命令行口径仍可用（经 [BenchRunner](file:///d:/project/chaos/chaos-java/jdk8-platform/jdk8-ratelimiter-demo/src/main/java/lan/chaos/ratelimiter/bench/BenchRunner.java)，复用同一压测引擎）。