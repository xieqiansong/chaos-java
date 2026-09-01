# 虚拟线程（JDK 21）：机制演示 + 压测量化

> 本工程分两层，从「为什么」到「收益多大」一次讲透：
> **机制演示层**（吞吐对比 / 载体调度观察 / pinning 复现 / 结构化并发 / ThreadLocal 语义）——为什么快、怎么跑、有什么坑、正确用法、使用边界；
> **压测量化层**（IO 密集 / 饱和拒绝 / pinning 代价 / CPU 边界 / HTTP 服务）——用可复跑的压测把收益与边界量化，数据见 [TEST_REPORT.md](TEST_REPORT.md)。
> 全部基于标准 JDK 并发 API，零外部中间件，开箱即跑。

## 1. 定位（一句话）

虚拟线程的核心机制是**阻塞时从载体线程卸载（unmount），让载体线程转去服务其他虚拟线程**——因此 IO 密集负载下可用极少量 OS 线程承载海量并发阻塞任务。

- **机制演示层**：五个对照实验，回答「机制 → 收益 → 坑 → 正确用法」，每个场景都有输入参数与可对比的输出指标（秒级）。
- **压测量化层**：五个压测场景，用同一套引擎跑出吞吐、延迟分位、拒绝数，回答「收益多大、边界在哪」（分钟级，一条命令自动出报告）。

## 2. 技术栈与入口

- **基线**：JDK 21 + Spring Boot 3.5.14（仅 starter，无 Web）
- **依赖**：零外部中间件，仅 JDK 并发 API
- **根包**：`lan.chaos.virtualthread`
- **入口**：`VirtualThreadApplication` + `runner.DemoRunner`（控制台分节打印输入→输出）
- **压测入口**：`bench.BenchRunner`（测试与命令行共用）；结果自动落到 `target/bench-results.md`
- **触发**：`mvn test`（断言核心机制 + 引擎冒烟）/ `mvn test -Dbench=true`（压测矩阵）/ `mvn spring-boot:run`（看主线演示）

## 3. 快速开始

```bash
# 单元测试（机制断言 + 引擎冒烟，无需任何中间件，秒级）
mvn -pl jdk21-tech/jdk21-virtualthread-demo -am test

# 压测矩阵（约 15s，自动出报告 target/bench-results.md）
mvn -pl jdk21-tech/jdk21-virtualthread-demo -am test -Dbench=true

# 控制台主线演示
mvn -pl jdk21-tech/jdk21-virtualthread-demo spring-boot:run

# 以应用方式跑压测（不加 --bench 则跑机制演示）
mvn -pl jdk21-tech/jdk21-virtualthread-demo spring-boot:run -Dspring-boot.run.arguments=--bench
```

预期输出（节选）：

- 吞吐对比：虚拟线程组总耗时/吞吐显著优于固定线程池组
- 载体观察：同时运行峰值远小于任务数，载体线程去重数 ≈ CPU 核数
- pinning：synchronized 版峰值并发≈载体线程数，ReentrantLock 版远超
- 结构化并发：并行总耗时≈单个任务；失败传播抛 `ExecutionException`；超时抛 `TimeoutException`
- ThreadLocal：默认继承父值（与平台线程一致），显式关闭后读到 `null`

## 4. 场景一览

| 场景 | 类 | 关键指标 | 一句话 |
|------|----|----------|--------|
| 吞吐对比 | `throughput.ThroughputCompare` | 总耗时/吞吐/峰值并发 | 平台线程池 vs 虚拟线程，IO 阻塞场景吞吐差异 |
| 载体调度观察 | `runtime.CarrierObservation` | 同时运行峰值/去重载体数 | 阻塞卸载、载体复用的运行时证据 |
| pinning 复现 | `pinning.PinningCompare` | 峰值并发/总耗时 | synchronized 临界区内阻塞钉住载体线程 |
| 结构化并发 | `structured.StructuredConcurrency` | 行为结果 | 并行/失败传播/超时统一收束子任务 |
| ThreadLocal 语义 | `threadlocal.ThreadLocalSemantics` | 读取结果 | 默认继承可继承上下文，可显式关闭 |

**压测量化层（`mvn test -Dbench=true`，数据见 [TEST_REPORT.md](TEST_REPORT.md)）**

| 场景 | 类 | 关键指标 | 实测结论 |
|------|----|----------|----------|
| IO 密集吞吐 | `bench.scenario.IoIntensiveBench` | 吞吐/p50/p99 | 虚拟线程快 **5.0～9.9 倍**；并发翻倍时平台池吞吐纹丝不动 |
| 饱和 → 拒绝 | `bench.scenario.SaturationBench` | 成功/拒绝/p99 | 平台池拒绝 9,400 个，虚拟线程 **0 拒绝** |
| pinning 代价 | `bench.scenario.PinningBench` | 吞吐/p99 | `synchronized` 比 `ReentrantLock` 慢 **67 倍**，虚拟线程被锁退化 |
| CPU 密集边界 | `bench.scenario.CpuBoundBench` | 吞吐 | 1.01 倍（噪声）——**无收益，别换** |
| HTTP 服务 | `bench.scenario.HttpServerBench` | QPS/p99 | 只换 executor 提升 **1.05～5.4 倍**（抖动 ±10~20%，看区间），池越小收益越大 |

## 5. 场景详解

### 5.1 吞吐对比（为什么要用）

- **关键 API**：`Executors.newVirtualThreadPerTaskExecutor()` vs `Executors.newFixedThreadPool(n)`；`ExecutorService` 的 try-with-resources 自动等待任务完成。
- **WHY（收益量化）**：固定线程池 N=16 时，2000 个各阻塞 5ms 的任务耗时 ≈ 2000×5/16 ≈ 625ms；虚拟线程阻塞即卸载，总耗时趋近单任务时长。峰值并发：平台线程恒等于线程数（阻塞把线程占满），虚拟线程远小于任务数。
- **注意**：这是机制演示而非生产压测；真实吞吐验证应结合压测工具与业务指标。

### 5.2 载体调度观察（怎么跑）

- **关键观察**：运行中虚拟线程的 `toString()` 形如 `VirtualThread[#42]/runnable@ForkJoinPool-1-worker-2`——`@` 后即载体线程名；阻塞中则显示 `sleeping`（已卸载、无载体）。
- **WHY（运行时机制）**：同时启动 N 个各阻塞一段时间的虚拟线程，统计「同时运行峰值」与「去重载体数」：峰值≈调度器并行度（默认=CPU 核数）而非 N，证明阻塞期间被卸载、载体被复用。
- **联动实验**：用 `-Djdk.virtualThreadScheduler.parallelism=8` 修改载体线程数，观察两项指标随并行度联动变化。

### 5.3 pinning 复现（生产坑）

- **WHY**：JDK 21 中虚拟线程执行 `synchronized` 临界区时会被「钉住」——临界区内阻塞也不卸载，占住载体线程不放（JDK 24 的 JEP 491 才修复）。本场景每个任务持有自己的锁，临界区内模拟 IO。
- **对照结果**：synchronized 版峰值并发被限制在载体线程数内、总耗时显著变长；ReentrantLock 版阻塞时正常卸载，峰值并发可远超载体线程数。
- **生产启示**：虚拟线程路径上避免在 `synchronized` 临界区内做阻塞 IO，或改用 `ReentrantLock`；定位 pinning 可用 `jcmd <pid> Thread.dump` 观察载体线程占用。

### 5.4 结构化并发（正确用法）

- **关键 API**：`StructuredTaskScope.ShutdownOnFailure`：`fork()` 提交子任务、`join()` / `joinUntil(deadline)` 等待、`throwIfFailed()` 聚合失败。
- **三种行为**：成功并行总耗时≈单个任务；任一子任务失败 → scope 关闭并取消其他子任务，`join()` 抛 `ExecutionException`；`joinUntil` 到点抛 `TimeoutException`。
- **WHY**：虚拟线程可大量创建，但「谁等谁」的并发编排易失控；结构化并发把子任务生命周期收束到父作用域，避免线程逃逸与空跑。

### 5.5 ThreadLocal 继承语义（使用边界）

- **WHY**：虚拟线程默认**继承**父线程的可继承上下文（`inheritInheritableThreadLocals` 默认 `true`，与平台线程一致）；但生产上不应依赖「线程本地变量隐式跨虚拟线程传递」——虚拟线程数量巨大、生命周期各异，隐式传递易导致上下文错乱与内存驻留。
- **对照**：默认读到父值；显式 `Thread.ofVirtual().inheritInheritableThreadLocals(false)` 关闭后子虚拟线程读到 `null`。
- **生产启示**：上下文应显式传递（参数 / 结构化并发作用域），而不是依赖 ThreadLocal 隐式继承。

## 6. 压测量化层（定量）

机制演示层证明了「阻塞会卸载」，压测层回答的是：**卸载带来的收益到底有多大、边界在哪**。五个场景共用一套引擎，参数集中在 `BenchOptions`。

### 6.1 引擎与指标口径

- `bench/BenchEngine`：**闭环负载**——`concurrency` 控制在途任务数，提交前取许可、完成后归还。一次性全量提交测到的是排队深度而非稳态，闭环才是稳定的压力模型。
- **延迟记「提交 → 完成」而非「执行开始 → 结束」**：排队是线程池饱和最先崩的地方，只看执行耗时会把排队代价藏起来，得出「平台线程也很快」的错误结论。
- `bench/ExecutorFactory`：平台池 = 固定线程 + **有界队列 + AbortPolicy**（贴近生产配置）；虚拟线程 = `newVirtualThreadPerTaskExecutor`（无队列上限，拒绝恒为 0）。
- 拒绝只在引擎侧计数一次（工厂只抛异常）——否则「抛出处 +1、捕获处 +1」会把拒绝数算成两倍。
- 预热（`warmupRounds`）抵消 JIT 与线程创建开销，结果丢弃。
- 指标：吞吐 = 成功数 / 墙钟耗时；p50/p99 = 端到端延迟分位；拒绝 = 被饱和策略拒掉的任务数；峰值并发 = 真正同时执行的任务数。

### 6.2 关键结论（详见 [TEST_REPORT.md](TEST_REPORT.md)）

| 场景 | 结论 | 量化 |
|------|------|------|
| IO 密集 | 虚拟线程吞吐随并发线性上涨，平台池被「线程数 / 单任务耗时」锁死 | 5.0～9.9 倍 |
| 饱和 | 平台池拒绝请求，虚拟线程零拒绝 | 9,400 → 0 |
| pinning | `synchronized` 临界区内阻塞让虚拟线程退化成平台线程 | 吞吐差 67 倍 |
| CPU 密集 | 无收益，瓶颈在核数不在线程模型 | 1.01 倍（噪声） |
| HTTP 服务 | 只换 executor 即可受益，池越小收益越大（抖动 ±10~20%） | 1.05～5.4 倍（区间） |

一句话：**虚拟线程买的是「等待」，不是「算力」**。IO 等待占比越高、并发越大，收益越大；CPU 密集别换；换的同时必须排查 `synchronized` 临界区内的阻塞调用。

### 6.3 与日常测试隔离

压测是分钟级且数据受机器负载影响，不适合进 CI 门禁：

- `BenchMarkTest` 用 `@EnabledIfSystemProperty(named = "bench", matches = "true")` 默认跳过，`mvn test -Dbench=true` 才触发。
- `BenchSanityTest` 用极小参数守住「引擎算得对」这条底线，随日常 `mvn test` 跑（秒级）——引擎若算错吞吐或漏任务，报告里所有数字都是废的。
- 断言只卡方向性结论（虚拟线程明显更快、pinning 明显更慢），不卡绝对值，避免机器差异误报。

## 7. 进阶方向（生产化，未实现，学习路径）

1. **真实链路压测**：把模拟 IO 换成真实远程调用（DB / HTTP / MQ），量化端到端收益；当前压测用的是可控阻塞，趋势可信但绝对值不能直接搬到线上。可结合压测工具（如 JMeter）观测「线程池饱和 → 排队 → 超时」到「吞吐稳定」的完整收益曲线。
2. **pinning 全面排查**：除 `synchronized` 外，`Object.wait()`、native 方法等也会 pin；用 `jcmd Thread.dump` 与 `-Djdk.tracePinnedThreads=full` 定位。
3. **线程池替换边界**：定时任务、有界队列限流、CPU 密集任务仍应保留平台线程语义；虚拟线程不替代线程池的全部职责（缺信号量/批量控制，可配合 `Semaphore` 限流）。
4. **与结构化并发的工程化结合**：超时/取消/错误聚合与网关超时、批量并行调用（如多路 MQ/DB 并发）整合。
5. **ThreadLocal 治理**：用 `ThreadLocal` 数量审计、改用参数传递/`ScopedValue`（JDK 24 预览）的迁移路径。

## 8. 设计要点

- **一个场景一个类**：机制层吞吐/调度/pinning/结构化/ThreadLocal 各自独立，互不耦合，可单独运行与断言。
- **两层解耦**：压测层只依赖 `common/`（`IoSimulator` / `ConcurrentCounter` / `LatencyRecorder`），不碰机制层的五个场景类，因此加压测没有改动任何既有断言。
- **指标可对照**：机制层输出「总耗时 / 吞吐 / 峰值并发」；压测层输出「吞吐 / p50 / p99 / 成功 / 失败 / 拒绝」，差异全部量化可见。
- **相对断言**：测试用对比关系（虚拟线程 < 平台线程一半、pinned < unlocked、峰值 < 载体数）而非绝对数值，CI 上不 flaky。
- **报告自动生成**：压测结果由 `MarkdownReporter` 直接渲染成 markdown，笔记里引用的数据与代码永远对得上。
- **零外部依赖**：仅 JDK 并发 API + Spring 空壳，无任何中间件（HTTP 场景用 JDK 内置 `HttpServer`，不引 Web 框架）。

## 目录

```
jdk21-virtualthread-demo/
├── pom.xml
├── README.md
├── TEST_REPORT.md                          # 压测报告（数据 + 结论）
└── src/
    ├── main/java/lan/chaos/virtualthread/
    │   ├── VirtualThreadApplication.java
    │   ├── common/                          # 两层共用的度量工具
    │   │   ├── constant/{Scenario,ExecutorMode}.java
    │   │   ├── model/{LoadResult,BenchResult,BenchCase}.java
    │   │   └── util/{IoSimulator,ConcurrentCounter,LatencyRecorder}.java
    │   ├── throughput/ThroughputCompare.java      # 机制层：平台线程池 vs 虚拟线程吞吐
    │   ├── runtime/CarrierObservation.java        # 机制层：载体线程挂载/卸载/复用
    │   ├── pinning/PinningCompare.java            # 机制层：synchronized vs ReentrantLock
    │   ├── structured/StructuredConcurrency.java  # 机制层：并行/失败传播/超时
    │   ├── threadlocal/ThreadLocalSemantics.java  # 机制层：继承默认值与显式关闭
    │   ├── bench/                                 # 压测层
    │   │   ├── BenchEngine.java                   # 闭环负载引擎：采集吞吐/延迟/拒绝
    │   │   ├── BenchOptions.java                  # 单轮参数（任务数/并发/池大小/队列…）
    │   │   ├── ExecutorFactory.java               # 按模式造执行器（有界队列+AbortPolicy / 虚拟线程）
    │   │   ├── BenchRunner.java                   # 跑矩阵 + 汇总
    │   │   ├── MarkdownReporter.java              # 结果渲染成 markdown
    │   │   └── scenario/
    │   │       ├── IoIntensiveBench.java          # A：IO 密集吞吐
    │   │       ├── SaturationBench.java           # B：饱和 → 排队 → 拒绝
    │   │       ├── PinningBench.java              # C：pinning 吞吐代价
    │   │       ├── CpuBoundBench.java             # D：CPU 密集边界
    │   │       └── HttpServerBench.java           # E：HTTP 服务 平台 vs 虚拟线程
    │   └── runner/{DemoRunner,BenchRunnerMain}.java
    └── test/java/lan/chaos/virtualthread/
        ├── *Test.java                             # 机制层断言（5 个）
        └── bench/
            ├── BenchMarkTest.java                 # 一键压测（-Dbench=true 才跑）
            └── BenchSanityTest.java               # 引擎冒烟（极小参数，默认跑）
```
