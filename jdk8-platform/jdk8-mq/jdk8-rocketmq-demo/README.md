# RocketMQ Demo

RocketMQ 常用场景演示模块，按功能子包分层，以 `DemoTest` 为统一触发入口（所有场景由测试触发，应用启动不自动收发消息）。

- 基础包：`lan.chaos.rocketmq`
- 触发入口：`DemoTest`（JUnit 5，已开启并行执行；**已去除类级 `@Disabled`**，无 broker 时由 `Assumptions` 优雅跳过）
- 配置：`application.yml`（NameServer 等，默认 `REDACTED:9876`）

> 所有场景均连真实 RocketMQ（`REDACTED:9876`）。请先用本项目 `docker-compose.yml` 起 NameServer + Broker：
> `docker-compose up -d`（停止：`docker-compose down`），再跑测试。

> 使用频率标注：`★★★ 高频`（几乎每个项目都会用）／`★★☆ 中频`（常见但不一定都有）／`★☆☆ 低频`（特定场景才需要）／`◆ 基础`（公共模块，非独立业务场景）。

## 目录结构

```
rocketmq-demo/src/main/java/lan/chaos/rocketmq/
├── RocketMqApplication.java            # 应用入口
├── common/                            # ◆ 公共抽象（幂等去重 / 本地事务存储接口+内存实现）
│   ├── idempotent/                    # 消费幂等去重（收进子包，不在 common 顶层）
│   │   ├── MessageIdStore.java        # 去重存储抽象
│   │   └── InMemoryMessageIdStore.java # 内存实现（生产换 Redis/DB 唯一键）
│   ├── model/Message.java            # 统一消息信封
│   └── util/MessageUtils.java        # 信封封装/解析工具
├── simple/                           # 基础收发 + 幂等消费 ★★★
│   ├── SimpleProducer.java
│   └── IdempotentConsumer.java
├── retry/                            # 重试 + 死信 ★★★
│   ├── RetryProducer.java
│   ├── RetryConsumer.java
│   └── DeadLetterConsumer.java
├── delay/                            # 延迟消息 ★★★
│   ├── DelayProducer.java
│   └── DelayConsumer.java
├── keyquery/                         # 按 Key 检索 ★★★
│   └── KeyQueryProducer.java
├── faulttolerant/                    # 发送侧容错 ★★★
│   └── FaultTolerantProducer.java
├── trace/                            # 消息轨迹 ★★★
│   └── TraceProducer.java
├── order/                            # 顺序消息（分区 + 全局） ★★☆
│   ├── OrderedProducer.java
│   ├── OrderedConsumer.java
│   ├── GlobalOrderProducer.java
│   └── GlobalOrderConsumer.java
├── transaction/                      # 事务消息 + 本地事务存储 ★★☆
│   ├── TransactionProducer.java
│   ├── TransactionListener.java
│   ├── TransactionConsumer.java
│   ├── LocalTxStore.java
│   └── InMemoryLocalTxStore.java
├── batch/                            # 批量发送 ★★☆
│   ├── BatchProducer.java
│   └── BatchConsumer.java
├── filter/                           # 消息过滤（Tag ★★☆ / SQL92 ★☆☆） ★★☆
│   ├── FilterProducer.java
│   ├── TagConsumer.java
│   └── SqlFilterConsumer.java
├── broadcast/                        # 广播模式 + 广播不重试 ★★☆
│   ├── BroadcastProducer.java
│   ├── BroadcastConsumer1.java
│   ├── BroadcastConsumer2.java
│   ├── BroadcastNoRetryProducer.java
│   └── BroadcastNoRetryConsumer.java
├── requestreply/                     # 请求-应答 (RPC) ★★☆
│   ├── RequestReplyProducer.java
│   └── RequestReplyConsumer.java
├── pull/                             # 主动拉取 (LitePull) ★★☆
│   ├── PullProducer.java
│   └── PullConsumer.java
├── acl/                              # ACL 鉴权 ★★☆
│   └── AclProducer.java
└── throttle/                         # 消费限流调优 ★★☆
    ├── ThrottleProducer.java
    └── ThrottleConsumer.java
```

## 场景一览（按使用频率排序）

`★★★ 高频`
- [基础收发·同步 simple](#3-基础收发与幂等消费-simple) → `DemoTest#sync`
- [幂等消费 simple](#3-基础收发与幂等消费-simple) → `DemoTest#sync/async/oneWay`
- [异步发送 simple](#3-基础收发与幂等消费-simple) → `DemoTest#async`
- [单向发送 simple](#3-基础收发与幂等消费-simple) → `DemoTest#oneWay`
- [重试 + 死信 retry](#4-重试与死信-retry) → `DemoTest#retry`
- [延迟消息 delay](#5-延迟消息-delay) → `DemoTest#delay`
- [按 Key 检索 keyquery](#6-按-key-检索-keyquery) → `DemoTest#keyQuery`
- [发送侧容错 faulttolerant](#7-发送侧容错-faulttolerant) → `DemoTest#faultTolerant`
- [消息轨迹 trace](#8-消息轨迹-trace) → `DemoTest#trace`

`★★☆ 中频`
- [顺序消息 order](#9-顺序消息-order) → `DemoTest#order`
- [全局顺序 order](#9-顺序消息-order) → `DemoTest#globalOrder`
- [事务消息 transaction](#10-事务消息-transaction) → `DemoTest#tx`
- [批量发送 batch](#11-批量发送-batch) → `DemoTest#batch`
- [消息过滤-Tag filter](#12-消息过滤-filter) → `DemoTest#filter`
- [广播模式 broadcast](#13-广播模式-broadcast) → `DemoTest#broadcast`
- [广播不重试 broadcast](#13-广播模式-broadcast) → `DemoTest#broadcastNoRetry`
- [请求-应答 requestreply](#14-请求应答-requestreply) → `DemoTest#requestReply`
- [主动拉取 pull](#15-主动拉取-pull) → `DemoTest#pull`
- [ACL 鉴权 acl](#16-acl-鉴权-acl) → `DemoTest#acl`
- [消费限流调优 throttle](#17-消费限流调优-throttle) → `DemoTest#throttle`

`★☆☆ 低频`
- [消息过滤-SQL92 filter](#12-消息过滤-filter) → `DemoTest#filter`（需 Broker 开启属性过滤）

`◆ 基础模块`
- [common 幂等去重 / 本地事务存储](#common-公共抽象)
- [message 统一消息信封](#message-统一消息信封)

---

### common 公共抽象 `◆`

幂等去重、本地事务状态存储、统一消息信封等的接口与内存实现，供多个场景复用（生产应替换为 Redis / 数据库）。

| 类 | 说明 |
|----|------|
| `common/model/Message.java` | 统一消息信封：消息载体（body + timestamp） |
| `common/util/MessageUtils.java` | 信封封装/解析工具（`pack`/`unpack`/`costMillis`，刻意不用 JSON 以免序列化开销污染耗时测量） |
| `common/MessageIdStore.java` | 幂等去重存储接口 |
| `common/InMemoryMessageIdStore.java` | 内存实现（演示用） |

---

### 3. 基础收发与幂等消费 simple `★★★`

最常用场景：同步 / 异步 / 单向三种发送方式，以及消费端基于 `MessageIdStore` 的业务侧幂等去重（RocketMQ 自动重试会带来重复投递，需业务去重）。

| 类 | 说明 |
|----|------|
| `simple/SimpleProducer.java` | 同步 `sendSync` / 异步 `sendAsync` / 单向 `sendOneWay` |
| `simple/IdempotentConsumer.java` | 消费时用 `MessageIdStore` 去重 |

触发：`DemoTest#sync` / `#async` / `#oneWay`

---

### 4. 重试与死信 retry `★★★`

消费失败自动重试（默认重试 16 次），耗尽后进入 `%DLQ%+消费组` 死信队列。本场景 `body` 含 `error` 触发失败，观察重试与死信。

| 类 | 说明 |
|----|------|
| `retry/RetryProducer.java` | 发送触发失败的消息 |
| `retry/RetryConsumer.java` | 故意抛异常，触发重试 |
| `retry/DeadLetterConsumer.java` | 监听死信队列 |

触发：`DemoTest#retry`

---

### 5. 延迟消息 delay `★★★`

RocketMQ 原生支持延迟消息（固定延迟级别 `1s/5s/10s/.../2h`）。与 Kafka 需自建方案不同，这里直接指定级别即可。

| 类 | 说明 |
|----|------|
| `delay/DelayProducer.java` | `sendDelay(msg, level)` 指定延迟级别 |
| `delay/DelayConsumer.java` | 正常消费延迟投递的消息 |

触发：`DemoTest#delay`（示例用 level=1 即延迟约 1s）

---

### 6. 按 Key 检索 keyquery `★★★`

发送时设 `keys`，可通过 `queryMsgByKey` 反向检索消息（依赖 Broker 的 index 文件）。常用于按业务单号查消息。

| 类 | 说明 |
|----|------|
| `keyquery/KeyQueryProducer.java` | `sendWithKey(key, body)` 发送并 `queryByKey(key)` 检索 |

触发：`DemoTest#keyQuery`

---

### 7. 发送侧容错 faulttolerant `★★★`

演示发送端高可用参数：重试次数、超时、Broker 故障延迟避让（`latencyMax` / `notAvailableDuration`）等，提升发送成功率。

| 类 | 说明 |
|----|------|
| `faulttolerant/FaultTolerantProducer.java` | 配置容错参数的生产者 |

触发：`DemoTest#faultTolerant`

---

### 8. 消息轨迹 trace `★★★`

开启消息轨迹（`hook` / `traceTopic`）后，可追踪消息从生产、存储到消费的完整链路，排查丢消息/重复问题。

| 类 | 说明 |
|----|------|
| `trace/TraceProducer.java` | 自带轨迹开关的生产者 |

触发：`DemoTest#trace`
> 注意：需 Broker 存在 `RMQ_SYS_TRACE_TOPIC`；否则仅丢失轨迹、不影响主消息。

---

### 9. 顺序消息 order `★★☆`

- **分区顺序**：相同 `shardingKey` 路由到同一队列，保证局部有序（最常用）。
- **全局顺序**：单队列 Topic，保证全局有序（吞吐受限，少用）。

| 类 | 说明 |
|----|------|
| `order/OrderedProducer.java` / `OrderedConsumer.java` | 分区顺序（按业务 key 路由队列） |
| `order/GlobalOrderProducer.java` / `GlobalOrderConsumer.java` | 全局顺序（单队列） |

触发：`DemoTest#order` / `#globalOrder`

---

### 10. 事务消息 transaction `★★☆`

RocketMQ 半消息 + 本地事务回查机制实现最终一致：`TransactionListener` 执行本地事务并返回状态，Broker 回调 `checkLocalTransaction` 回查。

| 类 | 说明 |
|----|------|
| `transaction/TransactionProducer.java` | 发送半消息、提交/回滚 |
| `transaction/TransactionListener.java` | 本地事务执行 + 回查 |
| `transaction/TransactionConsumer.java` | 消费已提交消息 |
| `transaction/LocalTxStore.java` / `InMemoryLocalTxStore.java` | 本地事务状态存储（接口+内存实现） |

触发：`DemoTest#tx`

---

### 11. 批量发送 batch `★★☆`

单次发送多条（`send(Collection<Message>)`），减少网络往返、提升吞吐。注意单批总大小限制（默认 ~4MB）与同批次消息需发往同一 Topic。

| 类 | 说明 |
|----|------|
| `batch/BatchProducer.java` | 攒批发送 |
| `batch/BatchConsumer.java` | 普通消费 |

触发：`DemoTest#batch`

---

### 12. 消息过滤 filter `★★☆`

- **Tag 过滤（中频）**：按 `Tag` 订阅过滤，最常用、零额外配置。
- **SQL92 过滤（低频）**：按消息属性写类 SQL 表达式过滤，需 Broker 开启属性过滤。

| 类 | 说明 |
|----|------|
| `filter/FilterProducer.java` | 发送带 Tag / 属性的消息 |
| `filter/TagConsumer.java` | 按 Tag 订阅消费 |
| `filter/SqlFilterConsumer.java` | 按 SQL92 表达式过滤消费 |

触发：`DemoTest#filter`
> 注意：`SqlFilterConsumer` 依赖 Broker 开启 `enablePropertyFilter=true`，否则监听启动失败；已用 `@Profile("sql92")` 隔离，不影响 Tag 消费者。启用：启动加 `--spring.profiles.active=sql92`。

---

### 13. 广播模式 broadcast `★★☆`

- **广播模式**：消息发给组内所有消费者各一份（每个消费者独立消费，且默认不重试）。
- **广播不重试**：广播模式下消费失败不重试，仅记日志。

| 类 | 说明 |
|----|------|
| `broadcast/BroadcastProducer.java` | 发送广播消息 |
| `broadcast/BroadcastConsumer1.java` / `BroadcastConsumer2.java` | 两个广播消费者各收一份 |
| `broadcast/BroadcastNoRetryProducer.java` / `BroadcastNoRetryConsumer.java` | 广播不重试演示 |

触发：`DemoTest#broadcast` / `#broadcastNoRetry`

---

### 14. 请求-应答 requestreply `★★☆`

基于 `request-reply` 语义实现 RPC 风格的同步往返：生产者发消息并阻塞等待消费者回包。

| 类 | 说明 |
|----|------|
| `requestreply/RequestReplyProducer.java` | 发送并等待响应 |
| `requestreply/RequestReplyConsumer.java` | 消费并回包 |

触发：`DemoTest#requestReply`

---

### 15. 主动拉取 pull `★★☆`

除默认 Push 消费外，可用 `LitePullConsumer` 主动控制拉取节奏（适合批处理、限速、与业务循环耦合的场景）。

| 类 | 说明 |
|----|------|
| `pull/PullProducer.java` | 发送消息 |
| `pull/PullConsumer.java` | `LitePullConsumer` 主动拉取演示 |

触发：`DemoTest#pull`

---

### 16. ACL 鉴权 acl `★★☆`

客户端配置 `AccessKey` / `SecretKey` 走 ACL 鉴权，控制 Topic 级读写权限。仅演示客户端写法。

| 类 | 说明 |
|----|------|
| `acl/AclProducer.java` | 带 ACL 凭证的生产者 |

触发：`DemoTest#acl`
> 注意：需 Broker 开启 ACL 并配好对应账号，否则报 `No permission` 鉴权失败。

---

### 17. 消费限流调优 throttle `★★☆`

通过并发线程数、拉取批次、消费耗时等参数调优消费吞吐，防止瞬时洪峰压垮下游。

| 类 | 说明 |
|----|------|
| `throttle/ThrottleProducer.java` | 发送一批消息 |
| `throttle/ThrottleConsumer.java` | 限流/调优参数下的消费者 |

触发：`DemoTest#throttle`

---

## 快速开始

```bash
# 1) 起 RocketMQ（NameServer + Broker，见 docker-compose.yml；需 Docker）
docker-compose up -d

# 2) 跑全部场景：无 broker 时自动跳过，有 broker 时真实发送并断言
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test

# 3) 跑单个场景
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test -Dtest=DemoTest#sync
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test -Dtest=DemoTest#broadcast
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test -Dtest=DemoTest#requestReply
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test -Dtest=DemoTest#pull
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test -Dtest=DemoTest#globalOrder
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test -Dtest=DemoTest#keyQuery
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test -Dtest=DemoTest#faultTolerant
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test -Dtest=DemoTest#trace
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test -Dtest=DemoTest#acl
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test -Dtest=DemoTest#throttle
mvn -o -pl jdk8-platform/jdk8-mq/jdk8-rocketmq-demo test -Dtest=DemoTest#broadcastNoRetry
```

> 触发入口 `DemoTest` 已去除类级 `@Disabled`：每个场景独立 `@Test`；类上挂 `BrokerReachableCondition`，
> 在 Spring 上下文启动**之前**用 TCP 探测 NameServer 是否可达，不可达时整类被**优雅跳过**（CI 无外部依赖时零误报，而非上下文崩溃报错），
> 可达时真实发送并用 `assertDoesNotThrow` 断言链路不抛异常。
> 末尾 `ThreadUtil.sleep` 仅用于等待异步消费端打印结果（可观察输出），并非测试入口。
>
> 消费者日志含 `耗时=xxxms`（消息从发送到被消费的端到端耗时）。

## 依赖外部配置的场景

| 场景 | 依赖 | 失败表现 |
|------|------|----------|
| 消息过滤-SQL92 | Broker `enablePropertyFilter=true` | 监听启动失败（已 `@Profile("sql92")` 隔离） |
| 消息轨迹 | Broker 存在 `RMQ_SYS_TRACE_TOPIC` | 仅丢轨迹，不影响主消息 |
| ACL 鉴权 | Broker 开启 ACL 并配账号 | `No permission` 鉴权失败（仅演示客户端写法） |

## 进阶方向（生产化考量，本项目未实现）

- **部署拓扑**：Broker 向 NameServer 注册的地址须宿主机可达（docker-compose 用 `network_mode: host` 规避），生产多 Broker 需配 `brokerIP1` 与多副本保障高可用。
- **鉴权与过滤**：ACL 需在 Broker 配好账号并开启；SQL92 过滤需 `enablePropertyFilter=true`，否则监听启动失败（已用 `@Profile("sql92")` 隔离）。
- **幂等 / 事务存储**：`MessageIdStore`、`LocalTxStore` 当前是内存实现，重启 / 多实例失效；生产替换为 Redis(SETNX+EX) 或数据库唯一键。
- **可观测**：接入消息轨迹（`RMQ_SYS_TRACE_TOPIC`）、死信队列告警、消费堆积监控，定位丢消息 / 重复 / 积压。
- **客户端参数**：发送容错（`latencyMax`/`notAvailableDuration`）、消费并发与拉取批次按下游能力调优，防瞬时洪峰压垮下游。

## 设计要点

- **统一消息封装**：`MessageUtils.pack(body)` 拼成 `时间戳毫秒|正文`，`unpack` 按首个 `|` 切分。刻意不用 JSON，避免序列化开销污染耗时测量。
- **触发收敛**：所有场景由 `DemoTest` 测试触发，应用启动不自动发消息；测试已开启并行执行（`@Execution(CONCURRENT)`）。
- **接口与实现分离**：`MessageIdStore`（幂等去重）、`LocalTxStore`（事务状态）均为接口，内存实现仅演示用，生产应替换为 Redis/数据库。
- **频率结论**：生产里真正"几乎必写"的是 **同步发送 + 幂等消费**；**重试/死信、延迟消息、按 Key 检索、发送容错、消息轨迹** 属于高频必备；顺序、事务、批量、过滤、广播、请求应答、主动拉取、ACL、限流调优按业务/治理需要选用；SQL92 过滤相对低频。
