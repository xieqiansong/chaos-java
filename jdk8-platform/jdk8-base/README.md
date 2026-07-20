# jdk8-base — Java 基础与杂项 Demo 合集

一句话定位：把四份历史模块（`java-guide-demos`、`tech-pdai-java-demos`、`chaos-test`、`javaagent`）合并到一个模块，方便统一编译与查阅。这里都是**纯 Java 知识点的可运行小例子**，没有 Web 外壳，绝大多数类自带 `main()`，直接运行即可观察输出。

> 说明：这是**知识点速查合集**，不是单一业务 Demo。它由四份历史代码合并而来，因此按「原始来源 + 知识点」双维度组织，包名保持原样（`lan.chaos.*`）。

---

## 技术栈

- JDK 8 / Maven（父模块 `jdk8-platform`）
- Hutool（Snowflake、IO、线程工具）
- JBoss Netty 3.x、Jedis、CGLib、JMH
- Leaf / TinyId 客户端（分布式 ID，需对应服务端，见下）

---

## 目录结构（按能力分包）

```
src/main/java/lan/chaos/
├── distributed/system/            # 来源：java-guide-demos
│   ├── distributed/id/            # 5 种分布式 ID 生成：UUID / Snowflake(Hutool) / Redis-INCR / Leaf / TinyId
│   └── paxos/                     # 一个最小可跑的 Basic Paxos 实现（Proposer/Acceptor/Learner + 本地传输）
├── java/                          # 来源：tech-pdai-java-demos
│   ├── base/                      # 反射、异常、模板方法小样
│   ├── spi/                       # JDK SPI 机制（ServiceLoader + META-INF/services）
│   ├── io/                        # BIO / NIO / AIO / Netty / 零拷贝 / 多路复用(Reactor)
│   └── juc/                       # 并发合集：AQS、锁、阻塞队列、Fork/Join、线程池、Phaser、CountDownLatch…
├── jvm/                           # 来源：chaos-test —— 类加载器、HeapOOM、CGLib 代理、wait/notify 调试
├── simple/web/                    # 来源：chaos-test —— 手写迷你 Servlet 容器（HttpServer + 请求/响应/Servlet 处理）
├── JavaTest.java                  # 来源：chaos-test —— Map 系列对比小测
├── SimpleBenchmark.java           # 来源：chaos-test —— JMH 字符串拼接基准
├── RealisticConcatBenchmark.java  # 来源：chaos-test —— JMH 更贴近真实的拼接基准
├── SimpleAgent.java               # 来源：javaagent —— JVM Premain Agent（挂 ClassFileTransformer 监控类加载）
└── DynamicAgent.java              # 来源：javaagent —— JVM Agent-Class（动态 attach，打印已加载类数）
```

---

## 快速开始

编译（在仓库根或 `jdk8-platform` 下）：

```bash
mvn -f jdk8-platform/pom.xml -pl jdk8-base -am compile
```

运行某个示例（示例均带 `main`），以 IDE 直接运行对应类最方便；命令行示例：

```bash
# JUC：CountDownLatch
mvn -pl jdk8-base exec:java -Dexec.mainClass=lan.chaos.java.juc.CountDownLatchDemo
# SPI：ServiceLoader 加载实现
mvn -pl jdk8-base exec:java -Dexec.mainClass=lan.chaos.java.spi.SPIDemo
# 迷你 Servlet 容器（默认监听端口见源码，浏览器访问即可）
mvn -pl jdk8-base exec:java -Dexec.mainClass=lan.chaos.simple.web.HttpServer
```

> 未在 pom 配置 `exec-maven-plugin` 时，上面命令需自行加插件或直接在 IDE 里点 Run；这类基础 demo 推荐 IDE 运行，边看边调。

---

## 场景一览（按上手难度）

| 分类 | 代表类 | 触发 | 看什么 |
|------|--------|------|--------|
| 分布式 ID | `distributed.id.SnowflakeHutoolExample` / `UUIDExample` | `main` | 控制台打印生成的 ID（无需外部依赖） |
| SPI | `java.spi.SPIDemo` | `main` | `ServiceLoader` 按 `META-INF/services` 加载 `DatabaseSearch`/`FileSearch` |
| JUC | `java.juc.*`（CountDownLatch/Semaphore/ForkJoin/Phaser…） | `main` | 多线程协作过程日志 |
| IO | `java.io.bio/nio/aio/netty/*` | 成对运行 Server + Client | Socket 收发、Reactor 多路复用 |
| JVM | `jvm.CustomClassLoader` / `jvm.JvmTest`（CGLib） | `main` | 自定义类加载、动态代理拦截 |
| 迷你容器 | `simple.web.HttpServer` | `main` + 浏览器 | 手写 Servlet 容器处理静态资源/Servlet |
| JMH 基准 | `SimpleBenchmark` / `RealisticConcatBenchmark` | `main` | 字符串拼接吞吐对比（JMH 报告） |
| Paxos | `distributed.system.paxos.demo.BasicPaxosDemo` | `main` | 一轮 Prepare/Accept/Chosen 共识过程 |
| Java Agent | `lan.chaos.SimpleAgent` / `lan.chaos.DynamicAgent` | `-javaagent:jar=xxx -jar app.jar` / attach | `premain` 拦截类加载 / `agentmain` 动态挂载打印类数 |

---

## 需要外部依赖的例子（少数）

大部分 demo 零依赖直接跑，以下几个需要外部服务/服务端，未起对应服务会连接失败，属正常：

- `distributed.id.RedisIdExample`：连 `localhost:30102` 的 Redis（`INCR` 发号），密码硬编码在源码里。
- `distributed.id.LeafSegmentExample`：依赖美团 **Leaf** 号段服务端（`SegmentService` 由 Spring 注入），单跑 `main` 不完整，仅作 API 示意。
- `distributed.id.TinyIdExample`：依赖滴滴 **TinyId** 服务端，`tinyid-client` 快照需本地仓库已有。

> IO 章节的 Server/Client 需要**同机成对启动**（先跑 Server 再跑 Client）。

---

## 设计要点 / 迁移说明

- **为什么合并**：三份历史代码都是「Java/分布式基础的碎片化练习」，各自独立成模块价值不大；合成 `jdk8-base` 后统一编译、便于速查，符合平台「一个能力一个模块」之外的「杂项归拢」诉求。
- **包名保持不变**：三者包根天然不冲突（`distributed.system.*` / `java.*` / 根级 `lan.chaos.*`），合并零改动即可共存。
- **原 `common` 模块的 JMH 依赖**：`chaos-test` 原先依赖 old 仓库的 `common` 模块（仅提供 JMH），迁移时直接把 `jmh-core` / `jmh-generator-annprocess` 收进本模块，去掉对 `common` 的耦合。
- **修正的一处 SPI Bug**：原 `META-INF/services` 文件名误写成 `lan.chaos.example.java.spi.Search`，与接口真实 FQN `lan.chaos.java.spi.Search` 不一致导致 `ServiceLoader` 加载不到实现；迁移时已改名修复，`SPIDemo` 现在能正确加载两个实现。
- **进阶方向**：分布式 ID 部分可补一个统一入口对比五种方案的吞吐/趋势；JMH 基准可加 `@BenchmarkMode`/`@Fork` 规范化并输出 JSON 报告。
- **原 `javaagent` 模块的合并**：`SimpleAgent`（Premain，挂 `ClassFileTransformer` 打印每个被加载的类名）与 `DynamicAgent`（Agent-Class，支持 `attach` 动态挂载后打印已加载类数）合并进 `lan.chaos` 包。`java.lang.instrument` 是 JDK 自带，无需额外依赖。`jdk8-base` 本身是普通 demo 模块、未在主 jar 配置 agent manifest；若要实跑，需将这两个类单独打成带 `Premain-Class: lan.chaos.SimpleAgent` / `Agent-Class: lan.chaos.DynamicAgent` 的 jar（参考原 `javaagent` 模块的 `maven-jar-plugin` 配置），再用 `-javaagent:agent.jar=hello -jar your-app.jar` 挂载。
