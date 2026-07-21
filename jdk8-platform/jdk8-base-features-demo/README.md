# JDK8 新特性学习模块（相对 JDK7）

## 一、一句话定位

用最少的代码把 **JDK8 相对 JDK7 的核心新特性**讲清楚：Lambda / Stream / Optional / 新日期时间 API / 接口默认方法 / 方法引用 / CompletableFuture / Base64 / StringJoiner。每个特性一个包、一个可运行 demo、一条可断言测试。

> 注：try-with-resources 是 **JDK7** 特性，不在此列。

## 二、技术栈与入口、触发方式

- 技术栈：纯 JDK8 + JUnit 5（仅测试作用域，由父 POM 的 spring-boot-dependencies BOM 管理版本）。**不引入任何业务框架**，保持对语言特性本身的聚焦。
- 根包：`lan.chaos.jdk8features`（能力即顶层包，共享模型/数据收进 `common/`）。
- 触发方式：
  - 控制台把玩：`NewFeaturesApp.main()`，分节打印每个特性的「输入 → 输出」。
  - 单元测试（首选、及格线）：每个特性包下一个 `*Test`，直接验证语义。

## 三、快速开始

```bash
# 编译 + 跑全部单元测试（离线）
cd jdk8-platform
mvn -o -pl jdk8-base-features-demo test

# 仅跑单个特性测试
mvn -o -pl jdk8-base-features-demo test -Dtest=StreamDemoTest

# 控制台分节打印所有特性演示
mvn -o -pl jdk8-base-features-demo compile exec:java -Dexec.mainClass=lan.chaos.jdk8features.NewFeaturesApp
```

预期：9 个测试全绿；控制台依次打印 Lambda / Stream / Optional / java.time / 接口默认方法 / 方法引用 / CompletableFuture / Base64 / StringJoiner 的输入与输出。

## 四、场景一览

| 能力包 | 特性 | 触发 |
|--------|------|------|
| `lambda` | Lambda 表达式 / 函数式接口 | `LambdaDemo.run()` / `LambdaDemoTest` |
| `stream` | Stream API（过滤/分组/聚合） | `StreamDemo.run()` / `StreamDemoTest` |
| `optional` | Optional 防空指针 | `OptionalDemo.run()` / `OptionalDemoTest` |
| `datetime` | java.time 新日期时间 | `DateTimeDemo.run()` / `DateTimeDemoTest` |
| `defaultmethod` | 接口默认/静态方法 | `DefaultMethodDemo.run()` / `DefaultMethodDemoTest` |
| `methodreference` | 方法引用（4 种形式） | `MethodReferenceDemo.run()` / `MethodReferenceDemoTest` |
| `completablefuture` | CompletableFuture 异步组合 | `CompletableFutureDemo.run()` / `CompletableFutureDemoTest` |
| `base64` | 标准库 Base64 | `Base64Demo.run()` / `Base64DemoTest` |
| `stringjoiner` | StringJoiner / String.join | `StringJoinerDemo.run()` / `StringJoinerDemoTest` |

## 五、场景详解（要点）

每个 demo 类顶部都有 WHY 注释（解决什么痛点、关键 API、生产坑）。摘几处：

- **Stream**：中间操作惰性、终止操作才触发；`Collectors.groupingBy` 分组、`toCollection(TreeSet::new)` 有序去重。
- **Optional**：优先 `orElseGet`（惰性）而非 `orElse`（急切）；避免 `get()`；`map/flatMap` 链式安全取值。
- **java.time**：`LocalDate` 等不可变、线程安全；`DateTimeFormatter` 线程安全；运算返回新对象。
- **接口默认方法**：实现类自动获得默认实现，可重写；多继承冲突需显式指定 `Interface.super.method()`。
- **CompletableFuture**：`supplyAsync` 异步产出，`thenCombine` 合并两个未来；生产多用 `thenAccept` 等非阻塞回调，慎用 `get()`。

## 六、进阶方向（生产化 / 无法用代码体现的部分）

以下属 JDK8 但**难以用几行代码演示**，列此作为学习路径（建议另开专题）：

- **HashMap 性能重构**：链表过长（≥8）转为红黑树，查询从 O(n) 降到 O(log n)。
- **ConcurrentHashMap 重写**：抛弃分段锁（Segment），改用 CAS + synchronized 细粒度锁，并发度更高。
- **Metaspace 取代 PermGen**：类元数据移到本地内存，默认不再有 `PermGen space` OOM，但需关注本地内存上限。
- **JVM 参数与 GC**：G1 成为默认关注点（JDK8 仍默认 Parallel GC，G1 需显式开启）；移除永久代后 GC 日志格式变化。
- **Nashorn JavaScript 引擎**（JDK8 引入，JDK11 标记弃用，JDK15 移除）：可在 JVM 内跑 JS。
- **并行数组排序**：`Arrays.parallelSort` 利用 ForkJoinPool 并行排序。
- **类型注解（Type Annotation）与重复注解（Repeating Annotation）**：`@NonNull` 可标注任意类型用法；同一注解可多次使用（需 `@Repeatable`）。
- **JMX、TLS、Unicode 6.2** 等底层增强。

## 七、设计要点

- **为什么按"能力包"组织**：一个特性一个包，单点聚焦、易检索；共享模型 `User` 与样例工厂 `SampleData` 统一收进 `common/`，避免重复造数据（见 `common/model`、`common/SampleData`）。
- **为什么用纯 JDK + 轻量测试**：新特性学习应排除框架噪音；每个特性一条 `*Test` 既验证语义又充当"可观察输出"，无需 Web 层。
- **频率结论**：Lambda/Stream/Optional 是 JDK8 之后日常最高频的写法；java.time 几乎取代所有 `Date/Calendar`；CompletableFuture 是异步编排起点；其余按需取用。
