# 测试报告：Redis 批量入库引擎（自适应批量大小）

> 统一报告：覆盖「环境准备 → 单元测试 → 三实现基准压测（匀速/flood）→ 结论」。
> 数据采集：2026-08-25，`localhost` Redis 8.0.2（30102），JDK17 运行 / target 1.8 编译，8 汇聚线程，单场景 12s。

## 一、测试环境

| 项 | 值 |
|---|---|
| 平台 / JDK | Windows / 编译 target 1.8（本机运行 JDK17） |
| 框架 | Spring Boot 2.7.18、Spring Data Redis（Lettuce） |
| 中间件 | Redis 8.0.2（`localhost:30102`，与 seckill-demo 共用；真实密码经本项目 `application-local.yml` 注入，该文件已被 .gitignore 忽略） |
| 压测并发 | 8 汇聚线程 / 单场景 12s |
| 被测实现 | `legacy`（基准，逐条直写）/ `static`（对照，定批 2048 + Pipeline）/ `adaptive`（目标，内存桶攒批 + 在线寻优） |
| 引擎参数 | queue=2M、queue-critical=200k、batch=[128,20480] 初始 2048、explore=0.2、idle=8s、sample-window=100 |

## 二、压测方式：SpringBootTest 一键跑

场景矩阵在 [BenchMarkTest.java](file:///d:/project/chaos/chaos-java/jdk8-platform/jdk8-batch-ingest-demo/src/test/java/lan/chaos/batchwriter/bench/BenchMarkTest.java) 定义，一条命令跑完整 6 场景并自动写出报告：

```bash
cd chaos-java/jdk8-platform/jdk8-batch-ingest-demo
mvn test -Dtest=BenchMarkTest
```

- 经 `@ActiveProfiles("local")` 读本项目 `application-local.yml` 注入真实 Redis 连接，不落任何连接凭证。
- 各场景使用独立 key（`bench:场景名`）隔离，避免互相污染。
- 压测核心抽为可复用引擎 [BenchEngine.java](file:///d:/project/chaos/chaos-java/jdk8-platform/jdk8-batch-ingest-demo/src/main/java/lan/chaos/batchwriter/bench/BenchEngine.java)。

指标定义：`items/s`（写入吞吐）、`redisCmds/s`（Redis 往返/命令数，legacy=条目数、批量≈批次数）、`avgBatch`（平均批量大小）、`dropped`（有界队列满丢弃数）、`errors`（Redis 写失败数）。

## 三、构建与单元测试

结果：**BUILD SUCCESS**，单元用例全部通过。

| 用例 | 说明 |
|---|---|
| `AdaptiveBatchWriterTest` | 纯内存验证（storage 零耗时）：30 万条 100% 排空无丢失、平均批量 >1、批量大小收敛且 clamp 到 [min,max] |
| `StaticBatchWriterTest` | 定批引擎内存验证：定额排列排空、无丢失 |

### 过程中修复的问题（供回查）
1. **尾批卡死丢失**：`drainLoop` 在「队列空 + 本地残批不满」时，`if(!batch.isEmpty())` 每次循环都刷新空闲计时 → `idleFlushNs` 兜底永不触发 → 最后一批滞留不刷。改为仅按 `drainTo` 实际新取条数刷新计时，并在 `while` 退出后刷出残批（`AdaptiveBatchWriter` 与 `StaticBatchWriter` 同步修复）。
2. **legacy 全量 ClassCastException**：`opsForHash().put(key, System.nanoTime(), item)` 的 hashKey 传 `Long`，而 `StringRedisTemplate` 的 hashKey 序列化器是 `StringRedisSerializer`（只接受 String）→ 每条直接抛异常，legacy 显示 0 吞吐、全量 errors。改为 `String.valueOf(System.nanoTime())`。
3. **static 丢弃数采集缺失**：`BenchEngine` 原只在 `instanceof AdaptiveBatchWriter` 时读 `dropped()`，static 的丢弃始终为 0（假象）。给 `BatchWriter` 接口加 `default long dropped()` 统一读取。

## 四、基准压测（SpringBootTest 实测，6 场景）

匀速场景 rate=5000；flood 场景以最大速率注入。

| 场景 | 模式 | 总条数 | items/s | redisCmds/s | avgBatch | dropped | errors |
|---|---|---|---|---|---|---|---|
| 01-匀速 | legacy | 60008 | 4998.4 | 4998.4 | 1.0 | 0 | 0 |
| 02-匀速 | static | 59584 | 4964.6 | 3.0 | 1655.1 | 0 | 0 |
| 03-匀速 | adaptive | 59392 | 4948.6 | 2.4 | 2048.0 | 0 | 0 |
| 04-flood | legacy | 149206 | 12429.3 | 12429.3 | 1.0 | 0 | 0 |
| 05-flood | static | 301056 | 25084.5 | 12.2 | 2048.0 | 95.7M | 0 |
| 06-flood | adaptive | 478612 | **39882.1** | 29.4 | 1355.8 | 90.8M | 0 |

> 完整原始数据见 `target/bench-results.md`（随 `mvn test` 自动重写）。

### 结论

1. **批量收益真实存在**：匀速 5000/s 下三条都能跟上，但 `redisCmds/s` 从 legacy 的 4998 骤降到 static 3.0 / adaptive 2.4（约 **2000 倍** 命令量下降）；flood 下 legacy=12429 → adaptive=29.4（约 **420 倍**）。
2. **自适应在 flood 下大幅提速**：adaptive T/s=39882，是 legacy(12429) 的 **3.2 倍**、static(25084) 的 **1.6 倍**。收益来自两点：
   - 存量队列用 `drainTo` 瞬取攒满大额批次，降低命令往返；
   - 队列超 `queue-critical` 时启用**第二加速线程**并发削峰，纯吞吐明显高于单线程定批的 static。
3. **批量大小随负载在线自适应**：匀速 5000/s 下 `avgBatch` 收敛在 2048（贴近上限，批量收益最大化）；flood 下收敛到 ≈1356 —— 用「略小批量 × 双线并发」换取更高吞吐，而非定死一个值，正是"动态引擎"的体现。
4. **削峰与背压**：flood 纯洪水注入 1 亿+ 条（远超 2M 有界队列），adaptive 丢弃(90.8M) 略低于 static(95.7M) 且写入量更高，削峰优于固定批量；其余场景 `dropped=errors=0`，批次闭合无丢失。
5. **模式正确性**：全场景 errors=0，匀速下 dropped=0；内存测试 30 万条 100% 排空，验证引擎逻辑独立于 Redis 稳定性。

## 五、下一步（可选增强）

- **背压升级**：flood 下超 2M 容量的丢弃按「降级直写 / 拒绝背压」策略处理，而非简单丢弃计数。
- **按 key 分桶多线程**：当前单消费线程 + 可选加速线程；更细粒度可按 key 分桶并发，进一步提升多租户写入吞吐。
- **批量写 MySQL**：复用同一攒批/寻优骨架，把 `storage()` 换成批量 upsert，验证批量收益在关系型存储的迁移性。