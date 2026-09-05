# jdk8-mqtt-demo  ★ A 类（MQTT / Eclipse Paho）

MQTT 核心能力演示模块，覆盖 **发布订阅模型、通配符订阅（`+` / `#`）、QoS 三等级、保留消息（Retained）、遗嘱消息（LWT）**，基于 Eclipse Paho MQTT v3 客户端 + 内存 Broker（moquette）。

- 基础包：`lan.chaos.mqtt`
- 技术栈：JDK 8 + Spring Boot 2.7.18 + Eclipse Paho `org.eclipse.paho.client.mqttv3` 1.2.5
- 验证入口（均随 `mvn test` 运行）：
  - `MqttMockScenarioTest`（*Test）：moquette **内存 Broker**，零外部依赖，`mvn test` 即绿，覆盖 基础收发 / 通配符 / QoS / 保留消息
  - `MqttBrokerTest`（*Test）：Testcontainers 拉真实 **mosquitto**，**无 Docker 时优雅跳过**，验证需真实语义的 遗嘱消息（LWT）/ 保留消息复验
- 本 demo 遵循仓库 [`AGENTS.md`](../../AGENTS.md) 的「AI 生成自检清单」，目录结构、注释风格、测试形态参照 ★ 标杆 `jdk8-localcache-demo`

> 使用频率标注：`★★★ 高频`／`★★☆ 中频`／`◆ 基础`（公共模块，非独立业务场景）。

## 目录结构

```
jdk8-mqtt-demo/
├── pom.xml                              # 继承 jdk8-mq；paho 客户端 + moquette(自包含) + testcontainers(真实 Broker *Test)
├── docker-compose.yml                   # eclipse-mosquitto:2.0.18，手动把玩用
├── src/main/java/lan/chaos/mqtt
│   ├── MqttApplication.java         # 启动类
│   ├── DemoRunner.java              # ApplicationRunner：启动后跑各场景（@Profile("!mock")）
│   ├── common/                      # ◆ 支撑（常量/工具/模型）
│   │   ├── constant/MqttConstants.java   # 主题 / QoS 等级命名常量
│   │   ├── model/SensorReading.java      # 演示实体（sample() 工厂）
│   │   └── util/
│   │       ├── MqttCollector.java        # 可复用接收收集器（回调收集 + 阻塞等待）
│   │       └── MqttClients.java          # 客户端工厂（屏蔽连接/订阅样板）
│   ├── pubsub/                      # ★★★ 发布订阅 / 通配符
│   │   ├── BasicPubSubDemo.java         # 基础发布订阅
│   │   └── WildcardSubDemo.java         # + 单层 / # 多层 通配符订阅
│   ├── qos/                         # ★★★ QoS 等级
│   │   └── QosDemo.java                 # QoS 0/1/2 发布与到达语义
│   ├── retained/                    # ★★☆ 保留消息
│   │   └── RetainedMessageDemo.java     # 后订阅者立即收到留存消息
│   └── lwt/                         # ★★☆ 遗嘱消息
│       └── LastWillDemo.java            # 异常断开触发 Broker 代发遗嘱
└── src/test/java/lan/chaos/mqtt
    ├── MqttMockScenarioTest.java    # *Test：moquette 内存 Broker 自包含
    └── MqttBrokerTest.java          # *Test：Testcontainers mosquitto（无 Docker 跳过）
```

> 设计要点：**能力场景是顶层包**（`pubsub` / `qos` / `retained` / `lwt`），`config/constant/model/util` 统一收进 `common/`。与 `jdk8-kafka-demo` / `jdk8-rabbitmq-demo` / `jdk8-rocketmq-demo` 包结构一致。

## 场景一览（按使用频率排序）

`★★★ 高频`
- [基础发布订阅](#1-基础发布订阅) → `roundTrip()`：发布者 → Broker → 订阅者 解耦模型
- [通配符订阅](#2-通配符订阅--与-) → `wildcard()`：`+` 单层 / `#` 多层 路由差异
- [QoS 等级](#3-qos-服务质量等级) → `qosRoundTrip(qos)`：0 至多一次 / 1 至少一次 / 2 恰好一次

`★★☆ 中频`
- [保留消息](#4-保留消息-retained) → `retainedDemo()`：后订阅者立即拿到最新值
- [遗嘱消息 LWT](#5-遗嘱消息-lwt) → `crashAndExpectWill()`：设备掉线 Broker 代发 offline

`◆ 基础模块`
- [MqttCollector / MqttClients](#mqttcollector--mqttclients-公共工具) → 接收收集器 + 客户端工厂

---

### 1. 基础发布订阅 `★★★`

MQTT 核心是「**发布者 → Broker（按主题路由）→ 订阅者**」的解耦模型，生产者与消费者只在「主题」上达成共识，彼此不知道对方地址/在线状态。

验证：`MqttMockScenarioTest#basic_pubsub_shouldDeliver` 断言 发布内容 == 订阅者收到内容。

**与 Kafka/RabbitMQ 差异**：Kafka 偏「日志流 / 分区重放」、RabbitMQ 偏「Broker 侧 Exchange 路由 + 复杂投递语义」；MQTT 极轻量、面向「海量设备 ↔ 少量服务端」的遥测/指令，主题即路由，几乎没有 Broker 侧拓扑配置。

---

### 2. 通配符订阅（`+` 与 `#`） `★★★`

主题用 `/` 分层：`+` 匹配**单层**、`#` 匹配**多层**。

- `demo/sensors/+/temp` 命中 `room1/temp`、`room2/temp`，**不命中** `room1/humidity`（层数不对）；
- `demo/sensors/#` 命中上述全部三条。

验证：`MqttMockScenarioTest#wildcard_shouldRouteByLevel` 断言 `+` 收到 2 条、`#` 收到 3 条、humidity 不进 `+`。

**典型用途**：温度监控服务订阅 `+/temp`；总控面板订阅 `#` 收全部遥测。

---

### 3. QoS（服务质量）等级 `★★★`

| QoS | 语义 | 确认机制 | 风险 |
|-----|------|----------|------|
| 0 | 至多一次 | 无确认，fire-and-forget | 可能丢 |
| 1 | 至少一次 | PUBACK | 不丢但**可能重复** |
| 2 | 恰好一次 | PUBREC/PUBREL/PUBCOMP 四段握手 | 不丢不重，最重 |

验证：`MqttMockScenarioTest#qos1_shouldDeliver` / `#qos0_shouldDeliver` 断言订阅者收到 1 条。

**坑点（面试常踩）**：QoS 是「发布→Broker→订阅」**两段独立协商，取两者较小值生效**。发布 QoS2、订阅 QoS0，最终订阅者只按 QoS0 收到（可能丢）。

---

### 4. 保留消息（Retained） `★★☆`

带 `retained=true` 的消息会被 Broker **按主题留存最后一份**；之后才上线的新订阅者，订阅瞬间**立即收到**这份留存——不必苦等下一次发布。

验证：`MqttMockScenarioTest#retained_shouldBeDeliveredToLateSubscriber` 断言 后订阅者收到的 == 发布值。

**典型用途**：设备「在线/离线」状态、配置快照——新面板一进来就能看到当前值。
**坑点**：保留消息每主题一份，新发布会覆盖；清除以「发布一条**空载荷**的 retained 消息」实现（本 demo 在 finally 中清理，避免污染后续测试）。

---

### 5. 遗嘱消息（LWT） `★★☆`

客户端连接时向 Broker 注册一条「遗言」；当客户端**异常断开**（网络断、进程崩、socket 被强行关闭，**不含**正常 DISCONNECT）时，Broker 自动把遗嘱发布到指定主题。

验证：`MqttBrokerTest#lwt_shouldPublishWillOnCrash`（Testcontainers 真实 Broker，无 Docker 跳过）断言收到遗嘱 `"offline"`。

**本 demo 触发关键**：用 `MqttAsyncClient.close(true)` **force 掐断底层 socket 且不发 DISCONNECT**——这正是「异常断开」，Broker 才会发遗嘱；正常 `disconnect()` 会告知 Broker 主动离开，不会触发。

**典型用途**：物联网设备掉线告警——连上时注册 `will=offline`，一掉线监控端立刻在状态主题看到 offline。

---

## 快速开始

```bash
# 1) 全部测试（内存 Broker 自包含 *Test 必过；Testcontainers 真实 Broker *Test 无 Docker 时自动跳过）
mvn -pl jdk8-mqtt-demo -am test

# 2) 手动把玩：起 mosquitto + 跑应用，观察各场景日志
cd jdk8-mqtt-demo && docker compose up -d
mvn -pl jdk8-mqtt-demo spring-boot:run
```

预期（控制台节选）：

```
========== MQTT Demo 启动，发布各场景样例消息 ==========
[pubsub] 发布 '{"sensorId":"demo-1",...}' -> 订阅者收到 '...'
[wildcard] + 通配收到 2 条(含 humidity=false)；# 通配收到 3 条
[qos] QoS=1 发布 'qos-1-payload' -> 订阅者收到 1 条
[qos] QoS=0 发布 'qos-0-payload' -> 订阅者收到 1 条
[retained] 发布保留消息 'retained-value' -> ...
[retained] 后订阅者立即收到保留消息 'retained-value'
[lwt] 设备 clientId=... 上线（已注册遗嘱 'offline'）
[lwt] Broker 在连接异常断开后发布遗嘱消息 'offline' -> ...
========== 各场景演示完毕，观察上方日志 ==========
```

## 进阶方向（生产考量，未写成独立 Demo）

- `◆` **会话与持久化订阅（Persistent Session）**：`cleanSession=false` + 客户端固定 clientId，离线期间的消息在重连后补发——与「保留消息」的区别是面向「离线补偿」而非「最新值」。
- `◆` **共享订阅（Shared Subscription）**：`$share/group/topic`，实现订阅端**负载均衡**（MQTT 5 原生，部分 3.1.1 Broker 也支持），类比 Kafka 消费组。
- `◆` **MQTT 5 vs 3.1.1**：MQTT 5 带来 Reason Code、User Property、消息过期、Topic Alias 等；本 demo 用 3.1.1（Paho v3 客户端）覆盖最通用语义。
- `◆` **安全**：TLS 加密、`username/password` 认证、`acl_file` 主题级授权（mosquitto）；生产务必开启，杜绝匿名。
- `◆` **与 Kafka / RabbitMQ 选型对比**：MQTT 是「设备接入协议」，通常前端接 EMQX/mosquitto 做协议转换，再桥接到 Kafka/RabbitMQ 做后端业务——它俩不是替代关系而是分层。

## 设计要点

- **双测试层**：`*Test` 用 moquette 内存 Broker 自包含（类比 `@EmbeddedKafka` / rabbitmq-mock），另一 `*Test` 用 Testcontainers 真实 mosquitto 验证 LWT 等需真实语义的场景；无 Docker 优雅跳过——既满足「及格线」又保证语义忠实。
- **能力即顶层包**：`pubsub` / `qos` / `retained` / `lwt` 各自聚焦一类知识点，`common/` 统一收常量/工具/模型。
- **客户端轻封装**：`MqttClients` 屏蔽连接/订阅样板、`MqttCollector` 用回调 + `CountDownLatch` 把异步到达变成可断言的同步结果，每个场景都「发布 → 收到 → 可观测」。
