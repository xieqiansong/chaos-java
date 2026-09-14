# JDK11 新特性学习模块（相对 JDK8，含 JDK9/10 引入的部分）

## 一、一句话定位

用最少的代码讲清 **JDK11 相对 JDK8 的核心新特性**（同时覆盖 JDK9/10 引入、在 JDK11 仍为"新版"的 API）：String 新方法、Files.readString/writeString、Optional 增强、Stream takeWhile/dropWhile、Predicate.not、Collection.toArray(IntFunction)、var(lambda)、标准 HttpClient。每个特性一个包、一个可运行 demo、一条可断言测试。

## 二、技术栈与入口、触发方式

- 技术栈：纯 JDK11 + JUnit 5（仅测试作用域，版本由父 POM 的 spring-boot-dependencies BOM 管理）。不引入业务框架，聚焦语言/标准库特性。
- 根包：`lan.chaos.jdk11features`（能力即顶层包，共享数据收进 `common/`）。
- 触发方式：
  - 控制台：`NewFeaturesApp.main()`，分节打印每个特性「输入 → 输出」。
  - 单元测试（首选）：每个特性包下一个 `*Test`。

## 三、快速开始

```bash
cd jdk11-platform
# 编译 + 跑全部单元测试（离线）
mvn -o -pl jdk11-base test

# 仅跑单个特性测试
mvn -o -pl jdk11-base test -Dtest=HttpClientDemoTest

# 控制台分节打印所有特性演示
mvn -o -pl jdk11-base compile exec:java -Dexec.mainClass=lan.chaos.jdk11features.NewFeaturesApp
```

预期：8 个测试全绿（其中 HttpClientDemoTest 用 JDK 内置 HttpServer 做本地真实收发，无需外网）；控制台依次打印 8 个特性的输入与输出。

## 四、场景一览

| 能力包 | 特性 | 触发 |
|--------|------|------|
| `string` | String 新方法（isBlank/strip/repeat/lines/indent） | `StringDemo.run()` / `StringDemoTest` |
| `files` | Files.readString / writeString、Path.of | `FilesDemo.run()` / `FilesDemoTest` |
| `optional` | Optional 增强（ifPresentOrElse/or/stream/isEmpty） | `OptionalDemo.run()` / `OptionalDemoTest` |
| `stream` | takeWhile / dropWhile / ofNullable | `StreamDemo.run()` / `StreamDemoTest` |
| `predicate` | Predicate.not | `PredicateDemo.run()` / `PredicateDemoTest` |
| `toarray` | Collection.toArray(IntFunction) | `ToArrayDemo.run()` / `ToArrayDemoTest` |
| `varlambda` | var 用于 lambda 参数 | `VarLambdaDemo.run()` / `VarLambdaDemoTest` |
| `httpclient` | 标准 HttpClient | `HttpClientDemo.run()` / `HttpClientDemoTest` |

## 五、场景详解（要点）

每个 demo 类顶部都有 WHY 注释。摘几处：

- **String**：`strip()` 去 Unicode 空白（含全角空格），`isBlank()` 认所有空白；`repeat/lines/indent` 补齐长期缺失的 API。
- **Files**：`writeString/readString` 一行完成读写（默认 UTF-8），`Path.of` 替代 `Paths.get`。
- **Optional**：`or` 为空改投别的 Optional；`stream()` 转 0/1 元素流便于 flatMap 串联；`ifPresentOrElse` 有/无各执行一支。
- **Stream**：`takeWhile` 取前缀至条件不成立；`dropWhile` 丢前缀；`ofNullable` 为 null 返回空流。
- **Predicate.not**：语义化取反；配合 `Objects::nonNull` 过滤 null（注意先去 null，否则对 null 调 `String::isEmpty` 会 NPE）。
- **toArray**：`list.toArray(String[]::new)` 一行得到正确类型数组。
- **var in lambda**：参数可用 `var`，从而能加注解或显式类型；要么全 var 要么全省略，不可混用。
- **HttpClient**：`HttpClient.newHttpClient()` + `HttpRequest.newBuilder(uri).GET()` + `client.send(req, BodyHandlers.ofString())`；支持 HTTP/2 与异步 `sendAsync`。

## 六、进阶方向（生产化 / 无法用代码体现的部分）

- **单文件源码直接运行（JDK11）**：`java Hello.java` 可直接跑单文件源码，无需先编译（仅限单文件、无外部依赖）。
- **Nest-Based Access（JEP 181）**：同一个顶层类内的嵌套类互访 private 成员不再需要编译器生成的桥接方法（accessibility 层面，难直接用几行展示）。
- **TLS 1.3 支持**：HttpClient 默认优先 TLS 1.3，握手更快更安全。
- **Epsilon 无操作 GC / ZGC 实验**：JDK11 引入 Epsilon（只分配不回收，用于性能测试/短命进程）与可伸缩低延迟 ZGC（实验）。
- **Unicode 10**：新增 emoji 等字符。
- **弃用/移除**：Nashorn 标记弃用（JDK15 移除）；移除 JavaFX、CORBA 等独立模块。
- **Flight Recorder / Mission Control** 开放使用。

## 七、设计要点

- **为什么按"能力包"组织**：一个特性一个包，单点聚焦；共享数据 `SampleData` 收进 `common/`，避免重复造数据。
- **HttpClient 为何用本地 HttpServer 做测试**：标准 HttpClient 需要网络端点，直接连外网在 CI 不稳；用 JDK 内置 `com.sun.net.httpserver.HttpServer` 起本地服务做真实收发，既验证 API 又零外部依赖。
- **频率结论**：String 新方法、Optional 增强、Files 读写、Collection.toArray 是日常高频；HttpClient 是替代 HttpURLConnection 的现代方案；var(lambda) 与 Stream 增强按场景取用。
