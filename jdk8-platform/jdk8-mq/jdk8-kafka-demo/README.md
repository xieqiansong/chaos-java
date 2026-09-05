# Kafka Demo

Kafka 核心能力演示模块，覆盖**基础收发、批量发送与消费、分区有序、Exactly-Once 事务、重试/死信、Header 消息过滤**等场景（基于 Spring Kafka 2.8.x + 原生 Kafka Clients）。**以 `@EmbeddedKafka` 自包含测试为核心验证手段**，无 Web 层，无需外部 Broker 即可跑测试。

- 基础包：`lan.chaos.kafka`
- 技术栈：Spring Boot 2.7.18 + Spring Kafka 2.8.x + Kafka Clients（与 Boot 版本自动对齐）
- 验证入口：`src/test/.../KafkaScenarioTest`（@EmbeddedKafka 启动内存 Broker，克隆即跑）
- 外部 Broker：提供 `docker-compose.yml`（KRaft 单节点），仅用于手动把玩主应用观察 `@KafkaListener` 日志

> 使用频率标注：`★★★ 高频`（几乎每个项目都会用）／`★★☆ 中频`（常见但不一定都有）／`★☆☆ 低频`（特定场景才需要）／`◆ 基础`（公共模块，非独立业务场景）。

## 目录结构

```
jdk8-kafka-demo/src/main/java/lan/chaos/kafka/
├── KafkaApplication.java                # 启动类
├── common/                              # ◆ 支撑（配置/常量/模型，边缘关注）
│   ├── config/
│   │   └── KafkaConfig.java            # KafkaAdmin 自动建 Topic + 事务 KafkaTemplate 配置 ◆
│   ├── constant/
│   │   └── KafkaConstants.java         # Topic 名称 / Group ID 命名常量
│   └── model/
│       └── OrderEvent.java             # 订单事件模型（有序/事务场景复用）
├── simple/                              # 基础收发 ★★★
│   ├── SimpleProducer.java             # 同步 / 异步 / 即忘 三种发送语义
│   └── SimpleConsumer.java             # 手动提交 offset + 消息队列
├── batch/                               # 批量发送与消费 ★★☆
│   ├── BatchProducer.java              # 显式攒批一次性发送
│   └── BatchConsumer.java              # 批量消费 List<ConsumerRecord>
├── order/                               # 分区有序 ★★☆
│   ├── OrderProducer.java              # 同 orderId → 同 partition → 消费有序
│   └── OrderConsumer.java              # 按 orderId 记录事件顺序供断言
├── transaction/                         # Exactly-Once 事务 ★★☆
│   ├── TransactionProducer.java        # @Transactional 控制提交/回滚
│   └── TransactionConsumer.java        # read_committed 仅消费已提交消息
├── retry/                               # 重试 + 死信主题 ★★★
│   ├── RetryProducer.java              # 发送可能失败的消息
│   ├── RetryConsumer.java              # 消费失败触发重试 → DLT
│   └── DeadLetterConsumer.java         # 收容重试耗尽的消息
└── filter/                              # Header 消息过滤 ★★☆
    ├── FilterProducer.java              # 发送带 type header 的消息
    └── FilterConsumer.java              # 按 header 做本地过滤（只处理 ORDER 类型）
└── isolate/                             # 多业务域并发隔离 ★☆☆
    ├── IsolateProducer.java             # 分别向快域/慢域 Topic 发送消息
    └── IsolateConsumer.java             # 单监听器多 Topic，按域路由到独立线程池
```

> 设计要点：**能力场景是顶层包**（`simple/batch/order/transaction/retry/filter/isolate`），`config/constant/model` 这类「配置与支撑」只是学习边缘关注，统一收进 `common/`（与 `jdk8-rocketmq-demo` 和 `jdk8-redis-demo` 的包结构一致）。

## 场景一览（按使用频率排序）

`★★★ 高频`

- [基础收发 simple](#1-基础收发-simple-) → `KafkaScenarioTest#simple_syncSend_shouldArrive` / `#simple_asyncSend_shouldArrive` / `#simple_fireAndForget_shouldArrive`
- [重试 + 死信 retry](#5-重试与死信-retry-) → `@EmbeddedKafka` 下真实跑通并断言：重试耗尽后消息进 DLT

`★★☆ 中频`

- [批量发送 batch](#2-批量发送与批量消费-batch-) → `KafkaScenarioTest#batch_shouldDeliverAll`
- [分区有序 order](#3-分区有序-order-) → `KafkaScenarioTest#order_sameKey_shouldArriveInOrder` / `#order_diffKey_mayArriveInterleaved`
- [Exactly-Once 事务 transaction](#4-exactly-once-事务-transaction-) → `KafkaScenarioTest#transaction_commit_shouldDeliverBothMessages` / `#transaction_rollback_shouldNotDeliver`
- [消息过滤 filter](#6-消息过滤-filter-) → `KafkaScenarioTest#filter_onlyOrderType_shouldBeConsumed`
- [多业务域并发隔离 isolate](#7-多业务域并发隔离-isolate-) → `KafkaScenarioTest#isolate_slowDomain_shouldNotBlockFastDomain`

---

### 1. 基础收发 simple `★★★`

最常用场景：同步/异步/即忘三种发送方式，消费端手动提交 offset（at-least-once）。

| 类 | 说明 |
|----|------|
| `simple/SimpleProducer.java` | 同步 `sendSync` / 异步 `sendAsync` / 即忘 `sendFireAndForget` |
| `simple/SimpleConsumer.java` | 手动 `ack.acknowledge()`，消息写入 `ConcurrentLinkedQueue` 供测试断言 |

验证：`KafkaScenarioTest` 三个用例，各发送一条消息后 await 检查 `SimpleConsumer.received` 含对应值。

**设计取舍：** 为什么用 `ConcurrentLinkedQueue` 而不是 `@KafkaListener` 直接 return / 用 `CountDownLatch`？因为 Kafka listener 异步消费，测试需要等待 + 断言实际消费结果；队列是最自然的可观察容器，线程安全且简单。

---

### 2. 批量发送与批量消费 batch `★★☆`

生产者一次性发送多条消息（减少 RTT），消费者开启 `spring.kafka.listener.type=batch` 一次拉取一批 `List<ConsumerRecord>`。

| 类 | 说明 |
|----|------|
| `batch/BatchProducer.java` | 攒 N 条消息一次性 `send` |
| `batch/BatchConsumer.java` | `onBatch(List<ConsumerRecord>)` 批量消费 |

验证：发送 5 条消息，await 等 `BatchConsumer.received` 到齐 5 条。

**坑点：** 批量模式下 offset 在批次处理完一次性提交，若批次处理中部分失败则整批 retry；生产环境应拆批或单条处理后分别 ack。

---

### 3. 分区有序 order `★★☆`

利用 Kafka「同 key → 同 partition → 单线程消费 → 有序」的机制，保证同一订单的生命周期事件按序消费。

| 类 | 说明 |
|----|------|
| `order/OrderProducer.java` | 以 `orderId` 为 key 发送 CREATE/PAY/SHIP/DONE 四个事件 |
| `order/OrderConsumer.java` | 按 `orderId` 归类事件，记录接收顺序供断言 |

验证：`order_sameKey_shouldArriveInOrder` 断言同一 orderId 的 4 个事件顺序为 CREATE→PAY→SHIP→DONE；`order_diffKey_mayArriveInterleaved` 验证不同单号各自独立有序。

**与 RocketMQ 差异：** Kafka 无全局顺序消息概念（除非单 partition topic）；RocketMQ 有 MessageQueueSelector + 全局顺序（单队列 Topic）。

---

### 4. Exactly-Once 事务 transaction `★★☆`

Kafka 事务保证「多分区/多 Topic 的原子写入」，搭配 `read_committed` 消费者只读已提交事务的消息。

| 类 | 说明 |
|----|------|
| `transaction/TransactionProducer.java` | `@Transactional` + `KafkaTransactionManager` 控制提交/回滚 |
| `transaction/TransactionConsumer.java` | `read_committed` 隔离级别消费 |

验证：`transaction_commit` 发送 2 条正常提交 → 断言消费到 2 条；`transaction_rollback` 发送 2 条但中间抛异常 → 断言消费者在 2s 后未收到任何回滚消息。

**与 RocketMQ 事务消息差异：** Kafka 的生产侧事务聚焦「写入原子性」，没有回查机制；RocketMQ 的半消息 + 本地事务回查适用于分布式事务最终一致。

---

### 5. 重试与死信 retry `★★★`

消费失败触发 Spring Kafka `DefaultErrorHandler` 重试，重试耗尽后 `DeadLetterPublishingRecoverer` 投递到死信主题。

| 类 | 说明 |
|----|------|
| `retry/RetryProducer.java` | 发送消息到 retry topic |
| `retry/RetryConsumer.java` | body 含 "error" → 抛异常 → 触发重试 |
| `retry/DeadLetterConsumer.java` | 收容 DLT 消息，记录日志 |

验证：`KafkaScenarioTest.retry_errorMessage_shouldEndUpInDLT` 在 `@EmbeddedKafka` 下真实跑通——专属 `retryContainerFactory`（`DefaultErrorHandler` + `DeadLetterPublishingRecoverer`，`FixedBackOff` 重试 1 次）使 `RetryConsumer` 失败抛异常 → 重试 → 投递到 `demo-retry-dlt`，由 `DeadLetterConsumer` 收容并断言。

**生产要点：** DLT 必须配合告警 + 人工/自动补偿，否则死信积压无感知。

---

### 6. 消息过滤 filter `★★☆`

Kafka 无服务端消息过滤，利用消息 Header 做本地判断（zero broker cost），实现按业务类型路由。

| 类 | 说明 |
|----|------|
| `filter/FilterProducer.java` | `ProducerRecord` header 写入 `type` 标签 |
| `filter/FilterConsumer.java` | 按 `type` header 过滤，只处理 `ORDER` 类型 |

验证：发送 ORDER / LOG / ALERT 各一条，断言 `FilterConsumer.received` 只有 1 条 ORDER 消息。

**与 RocketMQ Tag 过滤对比：** RocketMQ Tag 在 Broker 侧服务端过滤（消费者根本收不到非匹配消息，省带宽）；Kafka 需要消费者侧自行判断，消息仍被拉取到本地但业务跳过。

---

### 7. 多业务域并发隔离 isolate `★☆☆`

一个 `@KafkaListener` 同时订阅多个业务域 Topic，收到消息后按 Topic（业务域）路由到**该域专属线程池**异步处理，实现域间并发隔离——慢域任务阻塞不会拖垮快域。

| 类 | 说明 |
|----|------|
| `isolate/IsolateProducer.java` | 分别向快域 `TOPIC_ISOLATE_A` / 慢域 `TOPIC_ISOLATE_B` 发送消息 |
| `isolate/IsolateConsumer.java` | 单监听器订阅两域；`computeIfAbsent` 惰性创建域线程池，提交到对应池异步处理 |
| `common/config/KafkaConfig.java` | `isolateContainerFactory`：独立 ack 模式（RECORD）；域线程池在 consumer 内按 Topic 惰性创建 |

**为什么需要域隔离（核心痛点）：** 单监听器绑多 Topic 默认共用一个消费线程池。若某域处理慢（如重聚合/批量外呼），会占满线程、拖垮其它实时域。按域分配独立线程池后，慢域阻塞不影响快域。

**监听器里为什么必须异步提交到域线程池：** 若在 `@KafkaListener` 方法内直接 `Thread.sleep` 或做重计算，会占用 consumer 线程，导致整个监听器停止 poll——其它域消息也跟着卡住。提交到域线程池后 consumer 线程立即返回继续拉取。

**隔离证据（测试断言）：** 先发慢域消息（~800ms）、紧接发快域消息（~50ms）；断言「快域处理完成时间不晚于慢域」，即证明慢域未拖慢快域。

验证：`KafkaScenarioTest#isolate_slowDomain_shouldNotBlockFastDomain` —— await 两个域各处理 1 条，且 `IsolateConsumer.isFastFinishedBeforeOrWithSlow() == true`。

> 进阶方向：本场景以「Topic 即业务域」演示隔离。真实生产可进一步做「单 Topic 多业务域 + 按 key 哈希分域路由」「域级监控/限流」「慢域熔断降级」；Flink 实时链路的事件时间窗口、迟到容忍等机制可在此基础上类比（本 demo 仅覆盖消费侧，不引申 Flink 主 job）。

---

## 如何运行

```bash
# 编译 + 跑测试（无需外部 Kafka，@EmbeddedKafka 自包含）
mvn -pl jdk8-kafka-demo -am test

# 跑单个场景
mvn -pl jdk8-kafka-demo -am test -Dtest=KafkaScenarioTest#simple_syncSend_shouldArrive
mvn -pl jdk8-kafka-demo -am test -Dtest=KafkaScenarioTest#batch_shouldDeliverAll
mvn -pl jdk8-kafka-demo -am test -Dtest=KafkaScenarioTest#order_sameKey_shouldArriveInOrder
mvn -pl jdk8-kafka-demo -am test -Dtest=KafkaScenarioTest#transaction_commit_shouldDeliverBothMessages
mvn -pl jdk8-kafka-demo -am test -Dtest=KafkaScenarioTest#filter_onlyOrderType_shouldBeConsumed
```

或启动 Spring Boot 应用连接真实 Kafka Broker 手动把玩：

```bash
# 1) 启动 Kafka（KRaft 单节点）
cd jdk8-kafka-demo && docker compose up -d

# 2) 启动应用，观察各 @KafkaListener 控制台日志
mvn -pl jdk8-kafka-demo spring-boot:run
```

## 设计要点

- **自包含测试**：`@EmbeddedKafka` 启动内存 Broker，零外部依赖即可跑全部核心场景测试。与 RocketMQ demo 的 `@Disabled` + 外部 Broker 模式不同，这里真正做到了「克隆即编译、一条命令测试」。
- **按能力分包**：6 个能力场景为顶层包，`config/constant/model` 统一收进 `common/`（与 redis / rocketmq demo 一致的包结构）。
- **手动 offset 控制**：所有 consumer 关闭自动提交（`enable-auto-commit: false`），让测试能精确控制 ack 时机、观察消费语义。
- **事务隔离**：普通 producer 与事务 producer 分离（`transactionalKafkaTemplate` bean），互不干扰。
- **与 RocketMQ 的差异**：Kafka 没有原生延迟消息、定时消息、消息轨迹、请求-应答、广播模式（组内 consumer 按 partition 分配，天然点对点），这些是 Kafka 架构上的取舍。分区有序靠 key hash、事务聚焦写入原子性无回查——理解差异比记住 API 更重要。
