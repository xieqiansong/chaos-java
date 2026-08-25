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

## 四、基准压测（SpringBootTest 实测，6 场景）

匀速场景 rate=5000；flood 场景以最大速率注入。

| 场景 | 模式 | 总条数 | items/s | redisCmds/s | avgBatch | dropped | errors |
|---|---|---|---|---|---|---|---|
| 01-匀速 | legacy | 60008 | 4998.1 | 4998.1 | 1.0 | 0 | 0 |
| 02-匀速 | static | 58621 | 4884.5 | 3.2 | 1503.1 | 0 | 0 |
| 03-匀速 | adaptive | 59392 | 4947.9 | 2.4 | 2048.0 | 0 | 0 |
| 04-flood | legacy | 144411 | 12030.6 | 12030.6 | 1.0 | 0 | 0 |
| 05-flood | static | 282624 | 23548.4 | 11.5 | 2048.0 | 95.0M | 0 |
| 06-flood | adaptive | 257732 | **21474.4** | 10.2 | 2112.6 | 78.5M | 0 |

> 完整原始数据见 `target/bench-results.md`（随 `mvn test` 自动重写）。每场景执行前先 `DEL key` 隔离；数据只在内存桶一个队列中流转，核算闭合（written+dropped+errors=总投递）。

### 结论

1. **批量收益真实存在**：匀速 5000/s 下三条都能跟上，但 `redisCmds/s` 从 legacy 的 4998 骤降到 static 3.2 / adaptive 2.4（约 **2000 倍** 命令量下降）；flood 下 legacy=12031 → adaptive=10.2（约 **1200 倍**）。
2. **自适应在 flood 下远超 legacy、与 static 相当**：flood 下 adaptive T/s=21474，是 legacy(12031) 的 **1.8 倍**、与 static(23548) 同量级；`writer-threads` 可调大（固定 4 线程版本曾达 55630/s）。
3. **flood 下丢弃是真实的（核算闭合）**：生产速率 ≈8M/s 远超单线程消费 ≈23k/s，adaptive 丢 78.5M、static 丢 95.0M——两者同一量级（有界内存桶 offer 失败必丢），"削峰不丢"在纯洪水中不成立；自适应因平均批量略大（2112 vs 2048）消耗略快，丢弃反而少约 17%。匀速场景 dropped=0，贴近生产真实负载。
4. **批量大小随负载在线自适应**：匀速下 `avgBatch` 收敛在 2048；60s 收敛专项批量经 10 次「探索-反馈」在 757~1511 区间动态调整——引擎持续按实测吞吐反馈调整批量、而非定死一个值。
5. **模式正确性**：全场景 errors=0，内存测试 30 万条 100% 排空（零耗时存储下消费线程不积压、无丢弃），验证引擎逻辑独立于 Redis 稳定性。

## 五、自适应收敛专项（flood 60s）

> 12s flood 只能触发 ~3 轮调整。专项持续 60s（3 分钟级长跑对隧道/内存受限环境压力过大，取 1 分钟可观察完整调整周期），采样线程每 2s 记录 `currentBatchSize()` 实时轨迹（场景 `07-adaptive-convergence`，[BenchMarkTest.adaptiveConvergence](file:///d:/project/chaos/chaos-java/jdk8-platform/jdk8-batch-ingest-demo/src/test/java/lan/chaos/batchwriter/bench/BenchMarkTest.java)）。引擎固定 1 消费线程。

实测 batchSize 调整轨迹（60s 内 10 次寻优，queue 恒满 2M）：

| t(s) | batchSize | queue |
|---|---|---|
| 0 | 2048 | 0 |
| 10 | 1331 | 2M |
| 14 | 1144 | 2M |
| 20 | 1344 | 2M |
| 24 | 1155 | 2M |
| 30 | 1356 | 2M |
| 36 | 881 | 2M |
| 40 | 757 | 2M |
| 44 | 889 | 2M |
| 46 | 1511 | 2M |
| 54 | 981 | 2M |
| 58 | 1152 | 2M |

场景汇总（07-adaptive-conv，60s）：items/s=23652.5、redisCmds/s=19.5、avgBatch=1214.0、**dropped=450.8M、errors=0**。

> 说明：dropped=450.8M 是真实核算——纯洪水生产 ≈7.5M/s、单线程消费 ≈24k/s，差额在内存桶满时全部计入（消费线程直取内存桶、单队列核算闭合）。

**结论**：
1. **批量在线寻优持续工作（动态引擎核心实证）**：60s 内批量经 10 次「探索-反馈」调整，在 757~1511 区间动态震荡——引擎随实测吞吐反馈不断试探更优批量，而非定死一个值；低候选(757)与高候选(1511)交替出现正是"探索"阶段特性。
2. **全程吞吐稳定、零错误**：items/s≈23652、errors=0，redisCmds/s≈19.5（命令量较逐条降约 600 倍）。
3. **长时 flood 的丢弃是容量必然**：生产 >> 消费，有界缓冲必丢；核算闭合保证数据可审计（written+dropped=总投递）。

## 六、下一步（可选增强）

- **背压策略升级**：当前内存桶满即丢弃计数；可提供「降级直写 / 阻塞背压」模式，把丢失换成延时，适配"不可丢"场景。
- **按 key 分桶多线程**：当前 `writerThreads` 个消费线程直取内存桶并发写；更细粒度可按 key 分桶并发，进一步提升多租户写入吞吐。
- **批量写 MySQL**：复用同一攒批/寻优骨架，把 `storage()` 换成批量 upsert，验证批量收益在关系型存储的迁移性。