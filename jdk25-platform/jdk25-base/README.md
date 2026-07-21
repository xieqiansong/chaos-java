# JDK25 新特性学习模块（相对 JDK21）

## 一、一句话定位

用最少的代码讲清 **JDK25 相对 JDK21（覆盖 JDK22~25）的核心定稿新特性**：模块导入声明、灵活构造器体、隐式声明类/实例 main、Stream Gatherers、原始类型模式。其余特性（如永久禁用 Security Manager、虚拟线程去 pin、Class-File API、计算常量预览等）在第六节文字说明。

## 二、技术栈与入口、触发方式

- 技术栈：纯 JDK25 + JUnit 5（仅测试作用域，版本由父 POM 的 spring-boot-dependencies BOM 管理）。聚焦语言/标准库特性。
- 根包：`lan.chaos.jdk25features`（能力即顶层包，共享数据收进 `common/`）。
- 触发方式：
  - 单元测试（首选）：每个特性包下一个 `*Test`。
  - 控制台：`NewFeaturesApp.main()`，分节打印每个特性「输入 → 输出」。

## 三、快速开始

```bash
cd jdk25-platform
# 编译 + 跑全部单元测试（离线）
mvn -o -pl jdk25-base test

# 仅跑单个特性测试
mvn -o -pl jdk25-base test -Dtest=GatherersDemoTest

# 控制台分节打印所有特性演示
mvn -o -pl jdk25-base compile exec:java -Dexec.mainClass=lan.chaos.jdk25features.NewFeaturesApp
```

预期：全部测试全绿；控制台依次打印 5 个特性的输入与输出。

## 四、场景一览

| 能力包 | 特性 | 触发 |
|--------|------|------|
| `moduleimport` | 模块导入声明 `import module java.base;` | `ModuleImportDemo.run()` / `ModuleImportDemoTest` |
| `flexiblector` | 灵活构造器体（super 前后可写语句） | `FlexibleConstructorDemo.run()` / `FlexibleConstructorDemoTest` |
| `instancemain` | 隐式声明类 / 实例 main（免 static/args） | `InstanceMainDemo.main()` / `InstanceMainDemoTest` |
| `gatherers` | Stream Gatherers（自定义中间操作） | `GatherersDemo.run()` / `GatherersDemoTest` |
| `primitivepattern` | 原始类型模式（instanceof/switch 支持原始类型） | `PrimitivePatternDemo.run()` / `PrimitivePatternDemoTest` |

## 五、场景详解（要点）

每个 demo 类顶部都有 WHY 注释。摘几处：

- **模块导入声明**：`import module java.base;` 一行导入该模块导出的全部公共类型，省去逐条 `import java.util.*`。
- **灵活构造器体**：构造器里 `super()` 不必再是首行，可在调用父类前做参数校验、在 `super()` 之后给本类 final 字段赋值。
- **隐式声明类 / 实例 main**：启动类可省略 `static` 与 `String[] args`，`java` 启动器自动实例化再调用 —— 适合教学与极简入口。
- **Stream Gatherers**：`Stream.gather(Gatherer)` 插入可复用、可带状态的中间操作；内置 `Gatherers.windowFixed(n)` 做定长窗口，也可用 `Gatherer.of(...)` 自定义（本例"相邻差值"）。
- **原始类型模式**：`switch (Object)` 中可用 `case int i` / `case long l` / `case double d`，对装箱值自动拆箱匹配。

## 六、进阶方向（无法用稳定代码体现 / 其他已定稿特性）

- **永久禁用 Security Manager（JEP 486）**：`System.setSecurityManager(...)` 已被移除/禁用，`java.security` 相关 API 标记废弃。属于"移除包袱"，无需新代码。
- **虚拟线程不再 pin（JEP 491）**：JDK25 起虚拟线程在 `synchronized` 块/本地调用上不再被"钉"在载体线程，可更放心地用同步代码。可用 JDK21 模块的方式观察吞吐，但肉眼难量化，故未单独演示。
- **ZGC 分代默认可用（JEP 490 相关）**：`-XX:+UseZGC` 默认启用分代回收；属 JVM 调优，非代码特性。
- **Class-File API（JEP 484 定稿）**：`java.lang.classfile` 提供解析/生成 class 文件的纯 Java API，替代 ASM 等第三方库；适合字节码工具链，教学性价比低，未演示。
- **计算常量 Computed Constants（JEP 489，预览）**：`ComputedConstant` 惰性、缓存的"一次计算"值，适合高并发缓存；预览特性，未启用。

## 七、设计要点

- **为什么只挑 5 个定稿特性做代码演示**：JDK22~25 的定稿语言/API 特性里，这 5 个最易用少量代码讲清机制且可断言验证；其余多为 JVM/工具层（ZGC、Security Manager、Class-File）或预览特性，按规范"无法用代码描述就写 README"，放在第六节。
- **为什么按"能力包"组织**：一个特性一个包，单点聚焦；共享数据 `SampleData` 收进 `common/`，避免重复造数据。
- **频率结论**：模块导入声明与灵活构造器体在日常工程里很实用；Gatherers 适合复杂流处理；原始类型模式让数值分发更贴合底层；实例 main 主要用于教学与脚本。
