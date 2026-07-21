# jdk8-serialization-demo  ★ A 类（序列化/反序列化）

一句话定位：用**同一个 `User` 样例模型**横向对比三种 Java 序列化方案——**JDK 原生序列化、Jackson（JSON 文本）、Kryo（二进制）**，覆盖「往返正确性、体积对比、健壮性、生产坑」。纯内存、零外部依赖，跑测试或 `main` 即可看效果。

- 基础包：`lan.chaos.serialization`
- 技术栈：Spring Boot 2.7.18 + jackson-databind + kryo 5.5.0（JDK8 用 2.x；JDK11+ 才用 3.x）
- 验证入口：`src/test/.../*Test`（每个方案均可断言，无外部依赖，任何环境直接跑）
- 启动类：`SerializationApplication`（仅作 `main` 入口，分节打印三种方案「输入→输出」）

> 使用频率标注：`★★★ 高频`／`★★☆ 中频`／`◆ 基础`（支撑模块，非独立场景）。

## 目录结构

```
jdk8-serialization-demo/
├── pom.xml                                  # 继承 jdk8-platform，spring-boot-starter + jackson-databind + kryo
└── src/main/java/lan/chaos/serialization
    ├── SerializationApplication.java        # 启动类（控制台分节打印）
    ├── common/
    │   ├── model/User.java                   # 样例模型（实现 Serializable、自带 sampleUser() 工厂）
    │   └── util/ByteSizeUtil.java            # 字节数统计工具，用于横向对比体积
    ├── jdk/JdkSerializableDemo.java          # JDK 原生序列化（ObjectOutputStream）★★★
    ├── jackson/JacksonDemo.java              # Jackson JSON 文本序列化 ★★★
    └── kryo/KryoDemo.java                    # Kryo 二进制高性能序列化 ★★★
```

> 设计要点：**能力场景是顶层包**（`jdk/jackson/kryo`），`model/util` 这类支撑统一收进 `common/`。

## 场景一览（按使用频率排序）

`★★★ 高频`
- [JDK 原生序列化 jdk](#1-jdk-原生序列化-jdk) → `ObjectOutputStream` / `ObjectInputStream`，自带类型信息但体积最大
- [Jackson JSON 文本 jackson](#2-jackson-json-文本-jackson) → `writeValueAsString` / `readValue`，跨语言人类可读
- [Kryo 二进制 kryo](#3-kryo-二进制-kryo) → `writeObject` / `readObject`，体积最小性能最高

`◆ 支撑`
- [ByteSizeUtil 体积统计](#bytessizeutil-体积统计) / [User 样例模型](#user-样例模型)

---

### ByteSizeUtil 体积统计 `◆`

把序列化结果统一量化为「字节数」（JSON 取 UTF-8 字节、二进制取 `byte[]` 长度），供横向对比。是 `SerializationCompareTest` 断言「JDK > Jackson ≈ Kryo」的基础。

---

### User 样例模型 `◆`

同时实现 `Serializable`（JDK 原生需要）、提供无参构造（Jackson/Kryo 反射需要）、用 Lombok `@Data` 生成 `equals`（便于断言「往返后字段完全一致」）。覆盖 `Long/String/Date/List` 多种字段类型，确保三种方案都能处理复杂对象图。

---

### 1. JDK 原生序列化 jdk `★★★`

JDK 自带的 `ObjectOutputStream` / `ObjectInputStream`，**零依赖、自带完整类型信息**，但体积大、性能差、仅 JVM 可用。

- 关键 API：`new ObjectOutputStream(baos).writeObject(obj)` / `readObject()`
- 必须：`User implements Serializable` + `serialVersionUID`
- 生产坑：体积通常是 JSON 的 3~5 倍；类名/字段名全写进流，**反序列化即「不可信代码执行」入口**（攻击者可构造恶意流），务必配合白名单；跨语言不可用。

验证：见 `JdkSerializableDemoTest.roundTrip_preservesFields`（往返后 `equals` 一致）。

---

### 2. Jackson JSON 文本 jackson `★★★`

JSON 是「跨语言、人类可读、schema 宽松」的事实标准，RPC/HTTP/配置几乎都靠它。

- 关键 API：`objectMapper.writeValueAsString(obj)` / `readValue(json, Class)`
- 健壮性：`FAIL_ON_UNKNOWN_PROPERTIES=false` 容忍字段增减（接口演进不崩）
- 体积/可读性：`INDENT_OUTPUT` 仅美化，生产应关掉以省字节；日期默认时间戳，可按需关
- 生产坑：比二进制体积大、CPU 开销高；大对象/高频链路优先用二进制（见 Kryo）

验证：见 `JacksonDemoTest.roundTrip_preservesFields`（字段一致）、`toleratesMissingFields`（缺字段不崩）。

---

### 3. Kryo 二进制 kryo `★★★`

JVM 生态最快的二进制序列化之一，体积远小于 JSON/JDK，常用于会话复制、缓存、高性能 RPC。

- 关键 API：`kryo.writeObject(Output, obj)` / `readObject(Input, Class)`
- class 注册：`kryo.register(User.class)` 用数字 id 替代写全类名，更小更快；本 demo 用 `setRegistrationRequired(false)` 让 `Date`/`List` 等嵌套类型免登记
- 非线程安全：同一 Kryo 实例不能被多线程并发用，生产用 `ThreadLocal<Kryo>` 或对象池
- 生产坑：结果**非跨语言、不可人读**；class 结构变更需谨慎（版本兼容）；注册顺序影响字节布局

验证：见 `KryoDemoTest.roundTrip_preservesFields`（字段一致）、`smallerThanJdkNative`（体积小于 JDK）。

---

## 如何运行

```bash
# 1) 跑测试（核心验证，纯内存零外部依赖，任何环境直接过）
mvn -pl jdk8-serialization-demo -am test

# 2) 或看控制台「输入→输出」：直接运行 SerializationApplication.main
#    （IDE 里执行 main，或在命令行 mvn -pl jdk8-serialization-demo -am spring-boot:run）
```

预期（控制台节选）：

```
===== JDK 原生序列化 =====
字节数=...，往返后 name=Alice

===== Jackson（JSON 文本）=====
序列化结果: { "id" : 1, "name" : "Alice", ... }
往返后 name=Alice，roles=[USER, ADMIN]

===== Kryo（二进制）=====
字节数=...，往返后 name=Alice

体积对比(字节) JDK=xxx | Jackson=yyy | Kryo=zzz   # 一般 JDK > Jackson > Kryo
```

## 进阶方向（生产考量，未写成独立 Demo）

- `◆` **Protobuf / FlatBuffers**：跨语言 + 强 schema + 极致体积/性能，IDL 定义、向后兼容靠字段编号
- `◆` **Hessian2**：Dubbo 默认，跨语言二进制，体积与性能介于 JSON 与 Kryo 之间
- `◆` **JSON 性能**：Jackson 的 `Afterburner` / `Jackson-jr` 加速；大对象用流式 `JsonParser`/`JsonGenerator` 而非整树
- `◆` **安全**：JDK 原生反序列化必须白名单（`ObjectInputFilter`）；JSON 反序列化关掉 `enableDefaultTyping` 防 RCE
- `◆` **兼容性治理**：`serialVersionUID`、字段增删的向前/向后兼容策略、版本号灰度

## 设计要点

- **零外部依赖**：纯内存演示，开箱即跑，是「最快反应当前技术点」的极致形态。
- **同一模型三方案**：用同一个 `User` 让对比公平、可断言「往返后字段一致」，避免各写各的。
- **能力即顶层包**：`jdk/jackson/kryo` 各自聚焦一种方案，一个类讲清一个知识点 + WHY 注释。
- **频率结论**：JSON（跨语言可读）与 Kryo（高性能二进制）是生产最常用两种；JDK 原生序列化理解原理即可，新项目基本不首选。
