# jdk8-base — JDK8 新特性

一句话定位：JDK8 新特性学习模块，每个特性一个包 + 一条可断言 `*Test`（JUnit 5），纯 JDK 零外部依赖。

## 场景一览

| 特性 | 代表类 | 测试 |
|---|---|---|
| Lambda | `jdk8features.lambda.LambdaDemo` | `LambdaDemoTest` |
| Stream | `jdk8features.stream.StreamDemo` | `StreamDemoTest` |
| Optional | `jdk8features.optional.OptionalDemo` | `OptionalDemoTest` |
| 方法引用 | `jdk8features.methodreference.MethodReferenceDemo` | `MethodReferenceDemoTest` |
| 默认方法 | `jdk8features.defaultmethod.DefaultMethodDemo` | `DefaultMethodDemoTest` |
| 日期时间 | `jdk8features.datetime.DateTimeDemo` | `DateTimeDemoTest` |
| Base64 | `jdk8features.base64.Base64Demo` | `Base64DemoTest` |
| StringJoiner | `jdk8features.stringjoiner.StringJoinerDemo` | `StringJoinerDemoTest` |
| CompletableFuture | `jdk8features.completablefuture.CompletableFutureDemo` | `CompletableFutureDemoTest` |

共享样例数据在 `jdk8features.common`（`SampleData` / `User`）。

## 快速开始

```bash
mvn -pl java-core/jdk8-base test
```

或 IDE 运行 `NewFeaturesApp.main()` 分节打印各特性输出。

## 设计要点

本模块由原 `jdk8-base`（Java 基础与杂项合集）拆分收窄而来：分布式 ID / Paxos / Raft、IO / JUC / JVM / Agent / 迷你 Servlet 容器 / JMH 等已迁至各自技术域模块（见 `TECH-INDEX.md`），本模块仅保留 JDK8 新特性。
