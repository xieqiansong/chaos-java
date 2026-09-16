# jvm-demo — JVM 内功与 Java Agent

一句话定位：类加载器、堆 OOM、GC 行为对比、wait/notify 调试 + Java Agent（Premain / Agent-Class）+ CGLib 动态代理。

## 场景一览

| 主题 | 代表类 |
|---|---|
| 自定义类加载 | `jvm.CustomClassLoader` |
| 堆溢出 | `jvm.HeapOOM`（需调 `-Xmx` 观察） |
| GC 对比 | `jvm.gc.GarbageCollectorDemo` + `GarbageCollectorDemoTest` |
| wait/notify 调试 | `jvm.WaitNotifyDebugDemo` / `jvm.TestMain` |
| CGLib 代理 | `jvm.JvmTest` |
| Java Agent | `lan.chaos.SimpleAgent`（Premain）/ `lan.chaos.DynamicAgent`（Agent-Class） |

## Java Agent 实跑说明

两个 Agent 类需单独打成带 `Premain-Class: lan.chaos.SimpleAgent` / `Agent-Class: lan.chaos.DynamicAgent` 的 jar（`java.lang.instrument` 是 JDK 自带），再用 `-javaagent:agent.jar=hello -jar your-app.jar` 挂载；本模块未配置 agent manifest，仅保留类文件。

## 快速开始

```bash
mvn -pl java-core/jvm-demo test
```

IDE 运行对应 `main`。

## 设计要点

依赖 Hutool / CGLib。本模块自原 `jdk8-base` 拆分而来，包名保持不变。
