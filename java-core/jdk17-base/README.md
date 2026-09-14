# JDK17 新特性学习模块（相对 JDK11）

## 一、一句话定位

用最少的代码讲清 **JDK17 相对 JDK11 的核心新特性**：文本块、Record、密封类、instanceof 模式匹配、Switch 表达式、精确 NPE、Stream.toList/mapMulti、HexFormat、RandomGenerator 工厂。每个特性一个包、一个可运行 demo、一条可断言测试。

## 二、技术栈与入口、触发方式

- 技术栈：纯 JDK17 + JUnit 5（仅测试作用域，版本由父 POM 的 spring-boot-dependencies BOM 管理）。不引入业务框架，聚焦语言/标准库特性。
- 根包：`lan.chaos.jdk17features`（能力即顶层包，共享数据收进 `common/`）。
- 触发方式：
  - 单元测试（首选）：每个特性包下一个 `*Test`。
  - 控制台：`NewFeaturesApp.main()`，分节打印每个特性「输入 → 输出」。

## 三、快速开始

```bash
cd jdk17-platform
# 编译 + 跑全部单元测试（离线）
mvn -o -pl jdk17-base test

# 仅跑单个特性测试
mvn -o -pl jdk17-base test -Dtest=RecordDemoTest

# 控制台分节打印所有特性演示
mvn -o -pl jdk17-base compile exec:java -Dexec.mainClass=lan.chaos.jdk17features.NewFeaturesApp
```

预期：9 个测试全绿；控制台依次打印 9 个特性的输入与输出（含精确 NPE 的真实异常信息）。

## 四、场景一览

| 能力包 | 特性 | 触发 |
|--------|------|------|
| `textblock` | 文本块 `"""` + `formatted`/`translateEscapes` | `TextBlockDemo.run()` / `TextBlockDemoTest` |
| `record` | Record 记录类（不可变值对象、紧凑构造器） | `RecordDemo.run()` / `RecordDemoTest` |
| `sealed` | 密封类/接口（`sealed`/`permits`/final） | `SealedDemo.run()` / `SealedDemoTest` |
| `patterninstanceof` | instanceof 模式匹配 | `PatternInstanceOfDemo.run()` / `PatternInstanceOfDemoTest` |
| `switchexpression` | Switch 表达式（`->` / `yield` / 多分支合并） | `SwitchExpressionDemo.run()` / `SwitchExpressionDemoTest` |
| `helpfulnpe` | 精确 NullPointerException | `HelpfulNpeDemo.run()` / `HelpfulNpeDemoTest` |
| `streamtolist` | `Stream.toList()` / `mapMulti` | `StreamToListDemo.run()` / `StreamToListDemoTest` |
| `hexformat` | HexFormat 十六进制编解码 | `HexFormatDemo.run()` / `HexFormatDemoTest` |
| `randomgenerator` | RandomGenerator 工厂 | `RandomGeneratorDemo.run()` / `RandomGeneratorDemoTest` |

## 五、场景详解（要点）

每个 demo 类顶部都有 WHY 注释。摘几处：

- **文本块**：`"""..."""` 直接写多行，自动去除共同前导缩进；`formatted(...)` 做占位替换；`translateEscapes()` 把字面 `\n` 变成真换行。
- **Record**：`record Point(int x,int y)` 自动生成 final 字段、访问器、equals/hashCode/toString；可加普通方法、可写紧凑构造器做校验。适合不可变值对象。
- **密封类**：`sealed interface Shape permits Circle, Rectangle`，把"谁能实现我"写死；permitted 子类必须 `final`/`sealed`/`non-sealed`，使 instanceof 模式匹配可穷尽处理。
- **instanceof 模式匹配**：`if (o instanceof String s)` 一步判断+转型+绑定变量，可 `{@code &&}` 串联收窄。
- **Switch 表达式**：`->` 不贯穿、可直接返回；`case A, B -> ...` 合并分支；块内用 `yield` 返回值；必须穷尽。
- **精确 NPE**：JDK17 默认开启 JEP 358，NPE 信息指出"哪个引用为 null"（如 `Cannot read field "name" because "a.b.c" is null`）。
- **Stream.toList/mapMulti**：`toList()` 一行得到**不可变**列表；`mapMulti` 一对多展开，省中间集合。
- **HexFormat**：`HexFormat.of()`/`ofDelimiter`/`withPrefix` 统一十六进制格式化与解析，替代手写循环。
- **RandomGenerator**：`getDefault()` 取默认算法，`of("Xoshiro256PlusPlus")` 按名取算法，统一 `ints/longs/doubles` 流。

## 六、进阶方向（生产化 / 无法用代码体现的部分）

- **模式匹配 switch（JDK17 预览、JDK21 定稿）**：`switch (shape) { case Circle c -> ... }`，配合密封类可让编译器保证分支穷尽、无需 default。本模块为保持零预览依赖未启用，详见 JDK21 模块。
- **Record 模式（预览→定稿）**：在 switch/instanceof 中直接解构 `record`，如 `case Point(int x, int y)`。
- **密封类与序列化**：密封层次在反序列化时更安全（不能凭空造出未声明的子类）。
- **ZGC / Shenandoah**：JDK17 中 ZGC 已正式（不再是实验），亚毫秒级暂停，适合大堆低延迟场景。
- **始终严格浮点（JEP 306）**：JDK17 移除 `strictfp` 的语义差异，浮点运算在所有平台结果一致。
- **弃用 Applet / Security Manager 起步废弃**：为后续移除做准备。

## 七、设计要点

- **为什么按"能力包"组织**：一个特性一个包，单点聚焦；共享数据 `SampleData` 收进 `common/`，避免重复造数据。
- **为什么本模块不启用预览特性**：模式匹配 switch / Record 模式在 JDK17 仍是预览，启用需 `--enable-preview` 且会拖慢离线构建；这些特性在 JDK21 模块用正式语法完整演示，JDK17 只讲已定稿的部分，保证编译稳定。
- **频率结论**：文本块、Record、instanceof 模式匹配、Switch 表达式、Stream.toList 是日常高频；密封类适合框架/模型建模；HexFormat、RandomGenerator、精确 NPE 属于"好用且省心"的标准库增强。
