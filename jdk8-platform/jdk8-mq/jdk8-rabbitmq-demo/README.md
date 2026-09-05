# jdk8-rabbitmq-demo  ★ A 类（RabbitMQ / Spring AMQP）

RabbitMQ 核心能力演示模块，覆盖 **Exchange 四种类型路由（direct/topic/fanout/headers）、生产者确认（Publisher Confirm）与消费者手动 Ack、TTL + DLX 死信与延迟消息**，基于 Spring AMQP 2.4.x（Spring Boot 2.7.18）。

- 基础包：`lan.chaos.rabbitmq`
- 技术栈：Spring Boot 2.7.18 + Spring AMQP 2.4.x + RabbitMQ 3.12
- 验证入口（均随 `mvn test` 运行）：
  - `RabbitmqMockScenarioTest`（*Test）：rabbitmq-mock 内存 Broker，**零外部依赖**，`mvn test` 即绿，覆盖 Exchange 路由 + 基础收发
  - `RabbitmqBrokerTest`（*Test）：Testcontainers 拉真实 Broker，覆盖 publisher confirm / 手动 Ack / TTL+DLX / 延迟；**无 Docker 时优雅跳过**（与 `jdk8-elasticsearch-demo` 约定一致：集成测试即 *Test，随 `mvn test` 运行、无 Docker 跳过）
- 本 demo 遵循仓库 [`AGENTS.md`](../../AGENTS.md) 的「AI 生成自检清单」，目录结构、注释风格、测试形态参照 ★ 标杆 `jdk8-localcache-demo`

> 使用频率标注：`★★★ 高频`／`★★☆ 中频`／`◆ 基础`（公共模块，非独立业务场景）。

## 目录结构

```
jdk8-rabbitmq-demo/
├── pom.xml                              # 继承 jdk8-mq；spring-boot-starter-amqp + rabbitmq-mock(自包含) + testcontainers(真实 Broker *Test)
├── docker-compose.yml                   # rabbitmq:3.12-management，手动把玩用
├── src/main/java/lan/chaos/rabbitmq
│   ├── RabbitmqApplication.java         # 启动类
│   ├── DemoRunner.java                  # ApplicationRunner：启动后发布各场景样例（@Profile("!mock")）
│   ├── common/                          # ◆ 支撑（配置/常量/模型）
│   │   ├── config/RabbitConfig.java     # JSON 转换器 + 手动 Ack 容器工厂 + 全部拓扑 ◆
│   │   ├── constant/MqConstants.java     # 交换机/队列/路由键命名常量
│   │   └── model/OrderEvent.java         # 演示实体（sample() 工厂）
│   ├── exchange/                        # ★★★ Exchange 类型路由
│   │   ├── DirectExchangeDemo.java       # 精确匹配路由键
│   │   ├── TopicExchangeDemo.java        # 通配符 order.* / log.* / #
│   │   ├── FanoutExchangeDemo.java       # 广播到所有绑定队列
│   │   └── HeadersExchangeDemo.java      # 按消息 header 路由
│   ├── reliability/                     # ★★★ 消息可靠性：不丢 / 不重 / 有序
│   │   ├── PublisherConfirmDemo.java     # 不丢·生产侧：Publisher Confirm（waitForConfirms）
│   │   ├── ConsumerAckDemo.java          # 不丢·消费侧：发布 + 场景编排
│   │   ├── AckCollector.java             # 不丢·消费侧：@RabbitListener 手动 ack + nack 重入队（@Profile("!mock")）
│   │   ├── IdempotentDemo.java           # 不重：同一 orderId 发两次模拟重投
│   │   └── IdempotentCollector.java      # 不重：业务键去重，重复投递仅处理一次（@Profile("!mock")）
│   └── dlx/                             # ★★☆ TTL + DLX 死信与延迟消息
│       ├── DeadLetterDemo.java           # 工作队列 TTL 到期 → 死信到 DLT
│       ├── DeadLetterCollector.java      # DLT 监听器（@Profile("!mock")）
│       ├── DelayedMessageDemo.java       # TTL 队列 + DLX 实现延迟投递
│       └── DelayedMessageCollector.java  # 延迟目标队列监听器（@Profile("!mock")）
└── src/test/java/lan/chaos/rabbitmq
    ├── MockBrokerConfig.java             # 内存 Broker（rabbitmq-mock）覆盖 ConnectionFactory
    ├── RabbitmqMockScenarioTest.java     # *Test：Exchange 路由 + 基础收发（自包含）
    └── RabbitmqBrokerTest.java           # *Test：confirms/ack/DLX/延迟（Testcontainers，无 Docker 跳过）
```

> 设计要点：**能力场景是顶层包**（`exchange/reliability/dlx`），`config/constant/model` 统一收进 `common/`。与 `jdk8-kafka-demo` / `jdk8-rocketmq-demo` 包结构一致。

## 场景一览（按使用频率排序）

`★★★ 高频`
- [Direct 精确路由](#1-direct-交换机精确路由) → `route()`：routingKey 完全匹配才投递
- [Topic 通配符路由](#2-topic-交换机通配符路由) → `publishOrderCreated/publishLogInfo`：order.* / log.* / #
- [Fanout 广播](#3-fanout-交换机广播) → `broadcast()`：忽略 routingKey，投递所有绑定队列
- [Headers 按属性路由](#4-headers-交换机按-header-路由) → `publishTyped(type, ...)`：按消息 header 路由
- [生产者确认 Publisher Confirm](#5-生产者确认-publisher-confirm-) → `publishWithConfirm()`：waitForConfirms 确认到 Broker
- [消费者手动 Ack](#6-消费者手动-ack--nack-重入队) → `ConsumerAckDemo` + `AckCollector`：basicAck / basicNack(requeue)
- [幂等消费（不重）](#9-消息可靠性专题不丢--不重--有序) → `IdempotentDemo` + `IdempotentCollector`：业务键去重，重复投递仅处理一次

`★★☆ 中频`
- [TTL + DLX 死信](#7-ttl--dlx-死信队列) → 工作队列超时自动死信到 DLT（Broker 原生，无需抛异常）
- [延迟消息（TTL 队列 + DLX）](#8-延迟消息ttl-队列--dlx) → 缓冲队列到期死信到目标队列 = 延迟投递

`◆ 基础模块`
- [RabbitConfig](#rabbitconfig-公共配置) → JSON 转换器 + 手动 Ack 容器工厂 + 拓扑自动声明

---

### RabbitConfig 公共配置 `◆`

统一 `RabbitTemplate`（开启 `publisherConfirms`）、`Jackson2JsonMessageConverter`、`SimpleRabbitListenerContainerFactory`（`AcknowledgeMode.MANUAL`），并声明全部 Exchange/Queue/Binding，开箱即跑无需手动建组件。DLX/延迟拓扑带 `@Profile("!mock")`，只在真实 Broker 下声明，避免内存 Broker 不支持而拖垮 *Test。

---

### 1. Direct 交换机（精确路由） `★★★`

routingKey 必须**完全等于**绑定键才投递，点对点分发的最直接形态。

验证：`RabbitmqMockScenarioTest#direct_routing_shouldDeliver`（内存 Broker 发送并同步拉回，断言 orderId）。

---

### 2. Topic 交换机（通配符路由） `★★★`

binding key 用 `order.*` / `log.*` / `#` 模式，`order.created` 同时命中 `order.*` 与 `#` 两个队列。

验证：`RabbitmqMockScenarioTest#topic_shouldRouteByPattern` 断言：orders 队列只收订单、logs 队列只收日志、all 队列收全部；每个仅路由 1 份。

**与 Kafka 差异**：Kafka 分区有序靠 key hash；RabbitMQ topic 是 Broker 侧服务端路由，消费者根本收不到非匹配消息（省带宽），类似 RocketMQ Tag。

---

### 3. Fanout 交换机（广播） `★★★`

忽略 routingKey，把消息复制投递到所有绑定队列（每队列一份）。典型用途：清缓存、事件通知。

验证：`RabbitmqMockScenarioTest#fanout_shouldBroadcastToAllBoundQueues` 断言 A/B 两队列都收到。

---

### 4. Headers 交换机（按 header 路由） `★★★`

按消息 header 匹配（忽略 routingKey）。绑定 `where("type").equals("report")`；发布时写入 header `type=report` 才路由到对应队列。适合按属性而非主题路由。

验证：`RabbitmqMockScenarioTest#headers_shouldRouteByHeaderType` 断言 report 进 report 队列、notify 进 notify 队列，且互不串。

---

### 5. 生产者确认（Publisher Confirm） `★★★`

开启 `publisherConfirms=true` 后，每条消息有 deliveryTag，Broker 落盘/路由成功后回发 confirm。`RabbitTemplate.invoke` 内发布后 `waitForConfirms(timeout)` 阻塞等待全部确认。

验证：`RabbitmqBrokerTest#confirm_shouldAckToBroker`（真实 Broker，`waitForConfirms` 返回 true）。

**坑点**：confirm 只保证「到 Broker」，不保证「被消费」；路由失败也会 confirm 成功。若需感知不可路由，配合 `publisher-returns` / `mandatory`。

---

### 6. 消费者手动 Ack + nack 重入队 `★★★`

容器 `AcknowledgeMode.MANUAL` 下消息不自动 ack，需业务处理后 `basicAck`；失败可 `basicNack(requeue=true)` 重入队重试，或 `requeue=false` 进死信。

`AckCollector` 演示：首次投递故意 nack 重入队，重投后第二次处理完成 `basicAck`。

验证：`RabbitmqBrokerTest#consumerAck_shouldAckAfterRequeue` 断言最终 acked 且 requeue 次数 ≥ 1。

---

### 7. TTL + DLX 死信队列 `★★★`

工作队列声明 `x-message-ttl` + `x-dead-letter-exchange` + `x-dead-letter-routing-key`，消息超时无人消费即被 **Broker 自动死信**到 DLT——**无需业务代码抛异常**。这是与 Kafka 重试（代码层）的本质区别：RabbitMQ 死信是 Broker 原生能力。

验证：`RabbitmqBrokerTest#dlx_shouldDeadLetterAfterTtl`（约 1s 后消息出现在 DLT）。

**生产要点**：DLT 必须配合告警 + 人工/自动补偿，否则死信积压无感知。

---

### 8. 延迟消息（TTL 队列 + DLX） `★★★`

RabbitMQ 原生无延迟交换机（官方 `rabbitmq-delayed-message-exchange` 插件另论）。用「缓冲队列（`x-message-ttl`=延迟时长、`x-dead-letter-exchange`=目标交换机）+ 目标队列」组合：消息到期后被 Broker 死信到目标交换机再路由到目标队列 = 延迟 N 毫秒送达。

验证：`RabbitmqBrokerTest#delayed_shouldDeliverAfterDelay`（约 1.5s 后到达，断言耗时 ≥ 1s）。

**局限**：队列级 TTL 对所有消息同延迟；单条不同延迟需消息级 `expiration` 或延迟插件。

---

### 9. 消息可靠性专题（不丢 / 不重 / 有序） `★★★`

面试常问「消息怎么保证**不丢、不重、有序**」。本模块把三件事拆开演示，并区分**应用层**（代码可演示）与**中间件层**（需集群/队列属性配置，README 说明）。

| 维度 | 本 demo 演示（应用层） | 中间件层配置（仅说明） |
|------|----------------------|----------------------|
| **不丢·生产→Broker** | [`PublisherConfirmDemo`](#5-生产者确认-publisher-confirm-)：`waitForConfirms` 确认落盘 | `publisher-confirms=true` + 队列 `durable=true` + 消息 `deliveryMode=PERSISTENT`（持久化）；`publisher-returns`/`mandatory` 感知不可路由 |
| **不丢·Broker→消费** | [`ConsumerAckDemo`+`AckCollector`](#6-消费者手动-ack--nack-重入队)：业务完成才 `basicAck`，失败 `basicNack(requeue)` | 消费者 `AcknowledgeMode.MANUAL`；镜像/仲裁队列保证 Broker 高可用（见进阶方向） |
| **不重（幂等）** | `IdempotentDemo` + `IdempotentCollector`：以 `orderId` 业务键去重，重复投递仅真正处理一次 | 生产环境去重表用 Redis `SETNX+EX` / 数据库唯一键（多实例、重启仍有效） |
| **有序** | 单队列天然 FIFO（默认行为，无需专门代码） | 注意：多消费者 + `prefetch>1` 会破坏严格顺序；需严格有序用「单队列 + 单消费者」或消费端排序 |

**不重演示验证：** `RabbitmqBrokerTest#idempotent_shouldProcessOnlyOnce` —— 同一 orderId 发送 2 次，断言 `receivedCount==2`、`processedCount==1`。

**中间件层要点（面试话术）：**
- **不丢本质 = 确认机制 + 持久化 + 高可用队列**：confirm 只保证「到 Broker」，必须配合队列/消息持久化（否则 Broker 重启丢内存消息），再配合仲裁队列（quorum，替代 classic mirrored queue）防单点。
- **不重本质 = 业务幂等**：RabbitMQ 是 at-least-once，网络抖动/重投必然重复，只能靠消费端按业务键去重；本 demo 用内存 `Set` 演示原理，生产换分布式去重存储。
- **有序本质 = 单队列有序**：RabbitMQ 没有 Kafka 分区、RocketMQ MessageQueue 那样的「局部有序」概念，一个 queue 内本就 FIFO；要保证全局有序就「一 topic 一队列一消费者」，代价是失去并发。

---

## 快速开始

```bash
# 1) 全部测试（内存 Broker 自包含 *Test 必过；Testcontainers 真实 Broker *Test 无 Docker 时自动跳过）
mvn -pl jdk8-rabbitmq-demo -am test

# 2) 手动把玩：起 RabbitMQ + 跑应用，观察各 @RabbitListener 日志
cd jdk8-rabbitmq-demo && docker compose up -d
mvn -pl jdk8-rabbitmq-demo spring-boot:run
# 浏览器打开 http://localhost:15672  (guest/guest) 可看拓扑与消息流动
```

预期（控制台节选）：

```
========== RabbitMQ Demo 启动，发布各场景样例消息 ==========
[direct] 发布 orderId=demo-direct -> exchange=demo.direct.exchange, routingKey=demo.direct.routing
[direct] 从队列 demo.direct.queue 拉回: OrderEvent(orderId=demo-direct, ...)
[topic] 发布订单事件 orderId=demo-order -> routingKey=order.created
[fanout] 广播 orderId=demo-fanout -> 所有绑定队列
[ack] 第 1 次收到 orderId=demo-ack，处理失败 → basicNack(requeue=true) 重入队
[ack] 第 2 次收到 orderId=demo-ack，业务完成 → basicAck 确认
[dlx] 发布 orderId=demo-dlx -> 工作队列（TTL=1000ms 后自动死信到 DLT）
[dlx] 死信到达 DLT orderId=demo-dlx
[delay] 发布 orderId=demo-delayed -> 缓冲队列（TTL=1500ms 后投递到目标队列）
[delay] 延迟消息到达目标队列 orderId=demo-delayed
```

## 进阶方向（生产考量，未写成独立 Demo）

- `◆` **延迟插件**：`rabbitmq-delayed-message-exchange` 支持单条任意延迟，免「消息级 TTL 乱序」问题（队列级 TTL 下，早消息阻塞晚消息）
- `◆` **优先级队列 / 惰性队列**：`x-max-priority` 优先级、`x-queue-mode=lazy` 落盘抗积压
- `◆` **镜像/仲裁队列**：高可用（quorum queue 替代 classic mirrored queue）
- `◆` **消息可靠性全链路**：publisher confirm + 消费者手动 ack + 幂等消费（去重表）+ 死信补偿
- `◆` **与 Kafka 选型对比**：RabbitMQ 推模式、低延迟、Broker 侧路由/死信强；Kafka 拉模式、高吞吐、分区有序/日志重放

## 设计要点

- **双测试层**：`*Test` 用 rabbitmq-mock 内存 Broker 自包含（类比 Kafka `@EmbeddedKafka`），另一 `*Test` 用 Testcontainers 真实 Broker 验证 confirms/DLX 等需真实语义的场景；无 Docker 优雅跳过——既满足「及格线」又保证语义忠实。
- **能力即顶层包**：`exchange/reliability/dlx` 各自聚焦一类知识点，`common/` 统一收配置/常量/模型。
- **拓扑自动声明**：Exchange/Queue/Binding 以 `@Bean` 声明，开箱即跑，无需手动命令行建组件。
- **手动 Ack**：监听器容器统一 `MANUAL`，真实还原「业务处理完成再确认」的生产语义。
