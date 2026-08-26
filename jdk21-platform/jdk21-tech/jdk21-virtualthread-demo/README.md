# 虚拟线程（JDK 21）：吞吐对比 / 载体调度观察 / pinning 复现 / 结构化并发 / ThreadLocal 语义

> 用五个可复现场景演示 JDK 21 虚拟线程的完整机制：为什么快（IO 阻塞让出调度器）、怎么跑（载体线程挂载/卸载）、有什么坑（synchronized pinning）、正确用法（StructuredTaskScope）、使用边界（ThreadLocal 继承语义）。
> 全部基于标准 JDK 并发 API，零外部中间件，开箱即跑。

## 1. 定位（一句话）

虚拟线程的核心机制是**阻塞时从载体线程卸载（unmount），让载体线程转去服务其他虚拟线程**——因此 IO 密集负载下可用极少量 OS 线程承载海量并发阻塞任务。本 Demo 用五个对照实验把「机制 → 收益 → 坑 → 正确用法」完整量化，每个场景都有输入参数与可对比的输出指标。

## 2. 技术栈与入口

- **基线**：JDK 21 + Spring Boot 3.5.14（仅 starter，无 Web）
- **依赖**：零外部中间件，仅 JDK 并发 API
- **根包**：`lan.chaos.virtualthread`
- **入口**：`VirtualThreadApplication` + `runner.DemoRunner`（控制台分节打印输入→输出）
- **触发**：`mvn test`（断言核心机制）/ `mvn spring-boot:run`（看主线演示）

## 3. 快速开始

```bash
# 单元测试（无需任何中间件）
mvn -pl jdk21-tech/jdk21-virtualthread-demo -am test

# 控制台主线演示
mvn -pl jdk21-tech/jdk21-virtualthread-demo spring-boot:run
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

## 6. 进阶方向（生产化，未实现，学习路径）

1. **真压测**：结合压测工具（如 JMeter）与真实远程调用，量化「线程池饱和 → 排队 → 超时」到「吞吐稳定」的收益曲线；注意虚拟线程适合 IO 密集而非 CPU 密集。
2. **pinning 全面排查**：除 `synchronized` 外，`Object.wait()`、native 方法等也会 pin；用 `jcmd Thread.dump` 与 `-Djdk.tracePinnedThreads=full` 定位。
3. **线程池替换边界**：定时任务、有界队列限流、CPU 密集任务仍应保留平台线程语义；虚拟线程不替代线程池的全部职责（缺信号量/批量控制，可配合 `Semaphore` 限流）。
4. **与结构化并发的工程化结合**：超时/取消/错误聚合与网关超时、批量并行调用（如多路 MQ/DB 并发）整合。
5. **ThreadLocal 治理**：用 `ThreadLocal` 数量审计、改用参数传递/`ScopedValue`（JDK 24 预览）的迁移路径。

## 7. 设计要点

- **一个场景一个类**：吞吐/调度/pinning/结构化/ThreadLocal 各自独立，互不耦合，可单独运行与断言。
- **指标可对照**：所有对比场景输出「总耗时 / 吞吐 / 峰值并发」，机制差异全部量化可见。
- **相对断言**：测试用对比关系（虚拟线程 < 平台线程一半、pinned < unlocked、峰值 < 载体数）而非绝对数值，CI 上不 flaky。
- **零外部依赖**：仅 JDK 并发 API + Spring 空壳，无任何中间件。

## 目录

```
jdk21-virtualthread-demo/
├── pom.xml
├── README.md
└── src/
    ├── main/java/lan/chaos/virtualthread/
    │   ├── VirtualThreadApplication.java
    │   ├── common/
    │   │   ├── constant/Scenario.java
    │   │   ├── model/LoadResult.java
    │   │   └── util/{IoSimulator,ConcurrentCounter}.java
    │   ├── throughput/ThroughputCompare.java      # 平台线程池 vs 虚拟线程吞吐
    │   ├── runtime/CarrierObservation.java        # 载体线程挂载/卸载/复用
    │   ├── pinning/PinningCompare.java            # synchronized vs ReentrantLock
    │   ├── structured/StructuredConcurrency.java  # 并行/失败传播/超时
    │   ├── threadlocal/ThreadLocalSemantics.java  # 继承默认值与显式关闭
    │   └── runner/DemoRunner.java
    └── test/java/lan/chaos/virtualthread/*Test.java
```
