# JDK21 新特性学习模块（相对 JDK17）

## 一、一句话定位

用最少的代码讲清 **JDK21 相对 JDK17 的核心定稿新特性**：虚拟线程、SequencedCollection / SequencedMap、模式匹配 switch、Record 模式。预览特性（字符串模板、未命名变量、结构化并发、ScopedValue）在第六节用文字说明，因其需 `--enable-preview`，本模块保持零预览依赖以保证编译稳定。

## 二、技术栈与入口、触发方式

- 技术栈：纯 JDK21 + JUnit 5（仅测试作用域，版本由父 POM 的 spring-boot-dependencies BOM 管理）。聚焦语言/标准库特性，不引入业务框架。
- 根包：`lan.chaos.jdk21features`（能力即顶层包，共享数据收进 `common/`）。
- 触发方式：
  - 单元测试（首选）：每个特性包下一个 `*Test`。
  - 控制台：`NewFeaturesApp.main()`，分节打印每个特性「输入 → 输出」。

## 三、快速开始

```bash
cd jdk21-platform
# 编译 + 跑全部单元测试（离线）
mvn -o -pl jdk21-base-features-demo test

# 仅跑单个特性测试
mvn -o -pl jdk21-base-features-demo test -Dtest=VirtualThreadDemoTest

# 控制台分节打印所有特性演示
mvn -o -pl jdk21-base-features-demo compile exec:java -Dexec.mainClass=lan.chaos.jdk21features.NewFeaturesApp
```

预期：全部测试全绿（虚拟线程演示会提交 10000 个任务并全部完成）；控制台依次打印 5 个特性的输入与输出。

## 四、场景一览

| 能力包 | 特性 | 触发 |
|--------|------|------|
| `virtualthread` | 虚拟线程（每任务一线程的高并发） | `VirtualThreadDemo.run()` / `VirtualThreadDemoTest` |
| `sequencedcollection` | SequencedCollection（getFirst/Last/reversed） | `SequencedCollectionDemo.run()` / `SequencedCollectionDemoTest` |
| `sequencedmap` | SequencedMap（firstEntry/lastEntry/putFirst/Last） | `SequencedMapDemo.run()` / `SequencedMapDemoTest` |
| `patternswitch` | 模式匹配 switch（按类型/常量分发） | `PatternSwitchDemo.run()` / `PatternSwitchDemoTest` |
| `recordpattern` | Record 模式（instanceof / switch 解构） | `RecordPatternDemo.run()` / `RecordPatternDemoTest` |

## 五、场景详解（要点）

每个 demo 类顶部都有 WHY 注释。摘几处：

- **虚拟线程**：`Thread.startVirtualThread(...)` 直接起虚拟线程；`Executors.newVirtualThreadPerTaskExecutor()` 每任务一个虚拟线程，可轻松支撑万级并发而不必手动管理线程池；`thread.isVirtual()` 判别。
- **SequencedCollection**：`List`/`LinkedHashSet` 等统一实现该接口，提供 `getFirst()`/`getLast()`/`reversed()`（逆序视图，非拷贝），再也无需 `get(0)` 与临时反转。
- **SequencedMap**：`LinkedHashMap` 实现，提供 `firstEntry()`/`lastEntry()`/`putFirst()`/`putLast()`/`reversed()`。
- **模式匹配 switch**：`switch (o) { case Integer i -> ...; case String s -> ...; default -> ... }`，按类型直接绑定变量，无需预览参数（JDK21 已定稿）。
- **Record 模式**：`if (o instanceof Line(Point p1, Point p2))` 与 `case Line(Point p1, Point p2)` 直接解构 record，支持嵌套组件解构。

## 六、进阶方向（无法用稳定代码体现 / 预览特性）

JDK21 还有一批**预览/孵化**特性，开启需 `--enable-preview`（部分还需 `--add-modules`），本模块不启用以保证离线构建稳定；生产落地前请确认目标 JDK 版本是否已定稿：

- **字符串模板 String Templates（JEP 459，预览）**：`String name="world"; String s = STR."Hello \{name}";`，把插值和安全拼接（避免 SQL/HTML 注入）合二为一。
- **未命名变量与模式 `_`（JEP 443，预览）**：`case Foo(int _, int y) -> y;`、`for (var _ : list)`，忽略不关心的解构分量。
- **结构化并发 Structured Concurrency（JEP 453，预览/孵化）**：`try (var scope = new StructuredTaskScope.ShutdownOnFailure()) { ... }`，把多个子任务当作一个工作单元统一管理生命周期与异常。
- **作用域值 ScopedValue（JEP 446，预览）**：替代 `ThreadLocal` 的不可变、可继承上下文，配合虚拟线程更高效。
- **未命名类与实例主方法（JEP 445，预览）**：简化入门，单文件即可 `main` 运行。

## 七、设计要点

- **为什么只演示定稿特性**：JDK21 的"重量级"特性里，虚拟线程、Sequenced 集合、模式匹配 switch、Record 模式均已正式定稿，无需预览即可编译运行；剩余明星特性多为预览，强行启用 `--enable-preview` 会拖慢离线构建且随版本变动，故放 README 说明。
- **为什么按"能力包"组织**：一个特性一个包，单点聚焦；共享数据 `SampleData` 收进 `common/`，避免重复造数据。
- **频率结论**：虚拟线程与 Sequenced 集合是日常高频；模式匹配 switch / Record 模式显著提升表达力与安全性；预览特性适合在特定场景（模板防注入、结构化并发）深入。
