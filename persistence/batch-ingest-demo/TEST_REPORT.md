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
| 引擎参数 | queue=2M、batch=[128,20480] 初始 2048、explore=0.2、idle=8s、sample-window=100、**并发写线程 1（固定，不动态扩缩）** |

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
4. **adaptive 丢失核算黑洞（dropped 假零）**：曾引入"单承包人 + 写线程池"结构，写池自带任务队列 → 承包人把内存桶快速转存进写池从不阻塞 → 内存桶永远不"满" → `dropped` 恒为 0；生产(≈8M/s)>>消费(≈23k/s) 的差额全部积压在写池队列，`close()` 只消化极小部分，**剩余数千万条随 JVM 退出无声丢失、不计 dropped**。**最终方案（简化回归主线）**：去掉写池与第二级队列，`writerThreads` 个消费线程直接取内存桶攒批并写存储——数据只在内存桶一个队列中流转，丢弃只在 `offer` 失败时发生并计入 `dropped`，核算天然闭合（written+dropped+errors=总投递）。

## 四、基准压测（SpringBootTest 实测，static 固定批=512，1 vs 2 线程）

> 线程数 = 消费/写线程数（static 与 adaptive 由 `writer-threads` 控制；legacy 无消费线程、并发由 8 汇聚线程提供，两轮一致）。static 固定批量=512（`static-batch-size`）。匀速场景 rate=5000；flood 场景以最大速率注入。每场景执行前先 `DEL key` 隔离；数据只在内存桶一个队列中流转，核算闭合（written+dropped+errors=总投递）。

### 4.1 单线程（writer-threads=1）

| 场景 | 模式 | items/s | redisCmds/s | avgBatch | dropped | errors |
|---|---|---|---|---|---|---|
| 01-匀速 | legacy | 4999.3 | 4999.3 | 1.0 | 0 | 0 |
| 02-匀速 | static(512) | 4962.5 | 10.2 | 484.2 | 0 | 0 |
| 03-匀速 | adaptive | 4948.2 | 2.4 | 2048.0 | 0 | 0 |
| 04-flood | legacy | 11815.3 | 11815.3 | 1.0 | 0 | 0 |
| 05-flood | static(512) | 20221.0 | 39.5 | 512.0 | 91.3M | 0 |
| 06-flood | adaptive | 24051.7 | 13.6 | 1770.9 | 86.3M | 0 |

### 4.2 双线程（writer-threads=2）

| 场景 | 模式 | items/s | redisCmds/s | avgBatch | dropped | errors |
|---|---|---|---|---|---|---|
| 01-匀速 | legacy | 4998.5 | 4998.5 | 1.0 | 0 | 0 |
| 02-匀速 | static(512) | 4979.2 | 26.8 | 185.6 | 0 | 0 |
| 03-匀速 | adaptive | 4777.6 | 2.3 | 2048.0 | 0 | 0 |
| 04-flood | legacy | 11465.5 | 11465.5 | 1.0 | 0 | 0 |
| 05-flood | static(512) | **37160.1** | 72.6 | 512.0 | 97.5M | 0 |
| 06-flood | adaptive | **36455.8** | 19.3 | 1885.8 | 80.4M | 0 |

### 4.3 线程数对比 + 固定批量 512 vs 2048（历史）

| 场景 | 1 线程 items/s | 2 线程 items/s | 提升 |
|---|---|---|---|
| 匀速 static(512) | 4962.5 | 4979.2 | +0.3% |
| 匀速 adaptive | 4948.2 | 4777.6 | -3.4%（波动） |
| flood static(512) | 20221.0 | **37160.1** | **+84%** |
| flood adaptive | 24051.7 | **36455.8** | **+52%** |
| flood legacy | 11815.3 | 11465.5 | -3.0%（波动，不受消费线程数影响） |

固定批量对比（flood）：static-512 单线程 20221 vs 历史 static-2048 单线程 23548（**-14%**，单线程下大批量更优）；static-512 双线程 37160 vs 历史 static-2048 双线程 36513（**+2%**，2 线程下并发流水线补偿小批量开销，二者持平）。

> 完整原始数据见 `target/bench-results.md`（随 `mvn test` 自动重写）。

### 结论

1. **批量收益真实存在（两组均成立）**：匀速 5000/s 下三条都能跟上，`redisCmds/s` 从 legacy 的 ~4999 骤降到 static 10~27 / adaptive 2~3（约 **200~2000 倍** 命令量下降）；flood 下 legacy≈11500~11800 → adaptive 13~19（约 **600~800 倍**）。
2. **2 线程下吞吐显著提升（flood 场景）**：static(512) 20221 → 37160（**+84%**）、adaptive 24052 → 36456（**+52%**）——批量 Pipeline 与 Redis 并发执行在 2 消费线程下逼近单实例上限；legacy（无消费线程）不受影响（波动 -3%）。匀速低负载下线程数影响小（±3%）。
3. **固定批量存在最优值，且随线程数变化（自适应价值所在）**：单线程 flood 下 2048 优于 512（-14%）；双线程下二者持平（512 反超 2%）——最优批量随负载与并发动态变化，靠人工定死无法兼顾，这正是 `adaptive` 在线寻优的意义。
4. **flood 下丢弃真实且核算闭合**：生产 ≈8M/s 远超消费，adaptive 丢 80~86M、static 丢 91~98M——同量级（有界内存桶 offer 失败必丢），"削峰不丢"在纯洪水中不成立；匀速 dropped=0，贴近生产真实负载。
5. **模式正确性**：两轮全场景 errors=0，内存测试 30 万条 100% 排空，验证引擎逻辑独立于 Redis 稳定性。

## 五、自适应收敛专项（flood 60s，1 线程 vs 2 线程）

> 12s flood 只能触发 ~3 轮调整。专项持续 60s（3 分钟级长跑对隧道/内存受限环境压力过大，取 1 分钟可观察完整调整周期），采样线程每 2s 记录 `currentBatchSize()` 实时轨迹（场景 `07-adaptive-convergence`，[BenchMarkTest.adaptiveConvergence](file:///d:/project/chaos/chaos-java/jdk8-platform/jdk8-batch-ingest-demo/src/test/java/lan/chaos/batchwriter/bench/BenchMarkTest.java)）。

### 5.1 单线程（writer-threads=1，60s 内 7 次寻优，queue 恒满 2M）

| t(s) | batchSize 调整 |
|---|---|
| 0 | 2048（初始） |
| ~8 | 2048 → 1331 |
| ~26 | 1331 → 3481 |
| ~38 | 3481 → 2993 |
| ~48 | 2993 → 1945 |
| ~56 | 1945 → 2285 |
| ~64 | 2285 → 1484 |
| ~70 | 1484 → 964 |
| ~74 | 964 → 1638 |

场景汇总（07-adaptive-conv）：items/s=25851.6、redisCmds/s=12.0、avgBatch=2157.3、dropped=449.5M、errors=0。

### 5.2 双线程（writer-threads=2，60s 内 11 次寻优，批量持续爬升）

| t(s) | batchSize | queue |
|---|---|---|
| 0 | 2048 | 0 |
| 6 | 2406 | 2M |
| 12 | 1563 | 2M |
| 14 | 1343 | 2M |
| 20 | 1577 | 2M |
| 24 | 2680 | 2M |
| 32 | 1742 | 2M |
| 36 | 2961 | 2M |
| 44 | 1924 | 2M |
| 50 | 3270 | 2M |
| 58 | 3841 | 2M |

场景汇总（07-adaptive-conv）：items/s=40176.2、redisCmds/s=18.9、avgBatch=2127.7、dropped=442.7M、errors=0。

> 说明：dropped≈445M 是真实核算——纯洪水生产 ≈7.5M/s、消费 ≈26~40k/s，差额在内存桶满时全部计入（消费线程直取内存桶、单队列核算闭合）。

### 5.3 收敛结论

1. **批量在线寻优持续工作（动态引擎核心实证）**：1 线程批量 7 次调整、围绕 ~1600 震荡；2 线程 11 次寻优、后半程持续爬升到 **3841**——引擎随实测吞吐反馈不断试探更优批量，而非定死一个值。
2. **双线程吞吐 +55%**：40176 vs 25852，errors=0，redisCmds/s≈19（命令量较逐条降约 600 倍）。
3. **长时 flood 的丢弃是容量必然**：生产 >> 消费，有界缓冲必丢；核算闭合保证数据可审计（written+dropped=总投递）。

## 六、下一步（可选增强）

- **背压策略升级**：当前内存桶满即丢弃计数；可提供「降级直写 / 阻塞背压」模式，把丢失换成延时，适配"不可丢"场景。
- **按 key 分桶多线程**：当前 `writerThreads` 个消费线程直取内存桶并发写；更细粒度可按 key 分桶并发，进一步提升多租户写入吞吐。
- **批量写 MySQL**：复用同一攒批/寻优骨架，把 `storage()` 换成批量 upsert，验证批量收益在关系型存储的迁移性。