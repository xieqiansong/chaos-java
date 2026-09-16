# juc-demo — JUC 并发编程

一句话定位：Java 并发内功合集，覆盖 AQS / 锁 / 并发容器 / 阻塞队列 / 线程池 / ForkJoin / Phaser / 同步工具，纯 JDK 零外部依赖。

## 场景一览（代表）

| 主题 | 代表类 |
|---|---|
| AQS | `aqs.AbstractQueuedSynchronizerDemo` / `aqs.Depot` |
| 锁 | `ReentrantReadWriteLockDemo` / `ReentrantReadWriteLockExample` / `StampedLockDemo` / `LockDeadlockDemo` |
| 并发容器 | `ConcurrentHashMapDemo` / `ConcurrentLinkedQueueDemo` / `CopyOnWriteArrayListDemo` |
| 阻塞队列 | `blocking.queue.BlockingQueueExample` / `DelayQueueExample` / `SynchronousQueueDemo` |
| 线程池 | `thread.pool.SimpleThreadPool` / `WorkerPool` / `ScheduledThreadPoolDemo` / `FutureDemo` / `CallDemo` |
| Fork/Join | `fork.join.ForkJoinSumCalculator` / `FibonacciTest` / `FileCounter` / `ArraySquaringAction` |
| 同步工具 | `CountDownLatchDemo` / `CyclicBarrierDemo` / `SemaphoreDemo` / `ExchangerDemo` / `PhaserBasicDemo` / `PhaserDynamicDemo` / `PhaserTieredDemo` / `PhaserExample` |
| 其它 | `ThreadLocalExample` |

## 快速开始

全部带 `main`，IDE 直接运行即可观察多线程协作过程日志。

## 设计要点

纯 JDK 实现、零外部依赖。本模块自原 `jdk8-base` 拆分而来，包名保持不变。
