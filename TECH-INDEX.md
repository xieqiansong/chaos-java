# 技术点索引（按技术域检索）

> 按**技术域**组织的全仓库索引，覆盖 JDK 8 / 11 / 17 / 21 / 25 五个平台。
> 一份文件承载两件事：**前半部分**按技术域检索「已有什么」；**末尾**
> [待补技术点与建设优先级](#待补技术点与建设优先级) 是 roadmap，记录「还缺什么、按什么顺序补」。

**用法**：`技术点 → 模块 → 关键能力 → 配套笔记`。笔记列链接到
[`chaos-notes`](https://github.com/xieqiansong/chaos-notes) 仓库，其中 `performance/` 下的笔记含压测量化数据，
与本仓库的可运行模块一一对应——**代码与笔记双向可跳**，这是本仓库相对同类 demo 合集的主要差异。

**图例**：★ = 标杆 demo（新增 demo 的模板）｜`⚠️` = 对应笔记数据为示意框架、待实测回填

**模块链接**：`/` 开头为仓库根相对路径，可直接在 GitHub 上跳转。

---

## 目录

| 技术域 | 覆盖 |
|---|---|
| [Java 内核与 JDK 新特性](#java-内核与-jdk-新特性) | JVM / JUC / IO / 新特性版本矩阵 |
| [缓存](#缓存) | 本地缓存 / 多级缓存 / Redis |
| [消息队列](#消息队列) | Kafka / RocketMQ |
| [分布式与协调](#分布式与协调) | 分布式锁 / 分布式 ID / 分布式事务 / ZooKeeper / 一致性 |
| [微服务与治理](#微服务与治理) | Nacos / Sentinel / 限流 / 网关链路 |
| [数据持久层](#数据持久层) | MyBatis-Plus / 批量入库 / 数据同步 |
| [搜索](#搜索) | Elasticsearch |
| [安全与加密](#安全与加密) | Spring Security / JWT / OAuth2 / 国密 |
| [办公文档处理](#办公文档处理) | Excel / Word / PDF |
| [工程化与测试](#工程化与测试) | 单元测试 / 序列化 / 对象映射 / 定时任务 / 自动装配 |
| [AI 应用](#ai-应用) | Spring AI / MCP |
| [综合实战](#综合实战) | 秒杀 / 短链 / 微服务 |
| [延伸 · 其他仓库](#延伸--其他仓库) | 跨仓库的量化实践 |
| [待补技术点与建设优先级](#待补技术点与建设优先级) | roadmap：还缺什么、按什么顺序补 |

---

## Java 内核与 JDK 新特性

### 版本矩阵（本仓库的结构性差异点）

| 平台 | JDK | Spring Boot | 定位 |
|---|---|---|---|
| [`jdk8-platform`](jdk8-platform/README.md) | 8 | 2.7.18 | **默认位**——对 JDK 无强约束的技术一律归此，27 个模块 |
| [`jdk11-platform`](jdk11-platform/pom.xml) | 11 | 2.7.18 | 承载需 JDK 11+ 字节码的技术 |
| [`jdk17-platform`](jdk17-platform/pom.xml) | 17 | 3.5.14 | Spring AI / MCP 等需 JDK 17+ 的技术 |
| [`jdk21-platform`](jdk21-platform/pom.xml) | 21 | 3.5.14 | 虚拟线程 / 多级缓存 / 幂等 |
| [`jdk25-platform`](jdk25-platform/pom.xml) | 25 | **4.0.7** | JDK 25 新特性 |

> 归位规则：**技术对 JDK 有强约束 → 归入能跑通该技术的最低版本平台**；无约束 → 一律归 `jdk8-platform`。
> 详见 [`AGENTS.md`](AGENTS.md) 第八章。

**真实的 JDK 约束案例**（不是理论，是踩出来的）：

| 约束 | 结果 |
|---|---|
| `mybatis-plus-jsqlparser`（MyBatis-Plus 3.5.9+ 拆出的拦截器模块）要求 JDK 11+ 字节码 | MyBatis-Plus demo 从 `jdk8` **整体迁移至** `jdk11-platform`，旧副本保留原位 |
| Flink 1.17 不兼容 JDK 21 | Flink CDC demo 无法进 `jdk21-tech`，单独留在 `jdk8-tech` |

### 技术点

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| Java 内功合集 | [`jdk8-base`](jdk8-platform/jdk8-base/README.md) | 8 | 106 个可运行例子：JUC/AQS/线程池/Fork-Join/Phaser、BIO/NIO/AIO/Netty/零拷贝/Reactor、JVM 类加载/GC 对比、SPI、迷你 Servlet 容器、JMH、Java Agent（Premain/Agent-Class）、手写 Basic Paxos + Raft 模拟 | [JDK8 内核与并发内功](https://github.com/xieqiansong/chaos-notes/blob/main/notes/JDK8内核与并发内功.md) |
| 分布式 ID | `jdk8-base`（`java.base/id` 相关子包） | 8 | UUID / Snowflake / Redis-INCR / Leaf / TinyId | 见上 |
| 一致性算法 | `jdk8-base`（Paxos / Raft 子包） | 8 | 最小 Basic Paxos、Raft 选举与日志复制确定性模拟（内存状态机 + 可断言测试） | 见上 |
| JDK 8 新特性 | `jdk8-base` | 8 | Lambda / Stream / Optional / 方法引用 / 默认方法 / 日期时间 API / Base64 / CompletableFuture | [JDK8 到 25 新特性演进](https://github.com/xieqiansong/chaos-notes/blob/main/notes/JDK8到25新特性演进.md) |
| JDK 11 新特性 | [`jdk11-base`](jdk11-platform/jdk11-base/README.md) | 11 | String / Files / Optional / Stream 增强 / HttpClient / var（含 JDK 9、10 引入项） | 见上 |
| JDK 17 新特性 | [`jdk17-base`](jdk17-platform/jdk17-base/README.md) | 17 | 文本块 / Record / 密封类 / Switch 表达式 / instanceof 模式匹配 | 见上 |
| JDK 21 新特性 | [`jdk21-base`](jdk21-platform/jdk21-base/README.md) | 21 | 虚拟线程 / Sequenced 集合 / 模式匹配 switch / Record 模式 | 见上 |
| JDK 25 新特性 | [`jdk25-base`](jdk25-platform/jdk25-base/README.md) | 25 | 模块导入 / 灵活构造器体 / 隐式类 main / Stream Gatherers / 原始类型模式 | 见上 |
| **虚拟线程（压测量化）** | [`jdk21-virtualthread-demo`](jdk21-platform/jdk21-tech/jdk21-virtualthread-demo/README.md) | 21 | 机制演示 + 压测量化，含「不该换虚拟线程」的边界结论（CPU 密集、`synchronized` pinning） | **[虚拟线程 vs 平台线程：IO 密集压测与落地边界](https://github.com/xieqiansong/chaos-notes/blob/main/performance/虚拟线程vs平台线程-IO密集压测与落地边界.md)** |

---

## 缓存

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| 本地缓存 ★标杆 | [`jdk8-localcache-demo`](jdk8-platform/jdk8-localcache-demo/README.md) | 8 | Caffeine：基础读写 / 写入过期 / 容量淘汰 / 声明式 `@Cacheable`。纯内存零外部依赖，**后续所有 A 类 demo 的模板** | [本地缓存学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/本地缓存学习记录.md) |
| **多级缓存** | [`jdk21-multilevel-cache-demo`](jdk21-platform/jdk21-tech/jdk21-multilevel-cache-demo/README.md) | 21 | Caffeine L1 + Redis Hash L2 + 版本号一致性 | [多级缓存学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/多级缓存学习记录.md) |
| Redis 全场景 | [`jdk8-redis-demo`](jdk8-platform/jdk8-redis-demo/README.md) | 8 | Spring Data Redis + Lettuce，按能力分包：缓存 / 集合 / ZSet 排行榜 / 计数 / 分布式锁 / Lua 限流与扣库存 / Pipeline / PubSub | [Redis 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/Redis学习记录.md) |
| **批量入库引擎（压测量化）** | [`jdk8-batch-ingest-demo`](jdk8-platform/jdk8-batch-ingest-demo) | 8 | 内存桶攒批 + 水位触发 + **批量大小在线寻优**；对照 legacy 逐条 / static 定批。实测 flood 下 T/s ≈ 39882（legacy 的 3.2×），Redis 命令量降约 420 倍 | **[Redis 批量入库：自适应批量大小](https://github.com/xieqiansong/chaos-notes/blob/main/performance/Redis批量入库-自适应批量大小.md)** |

---

## 消息队列

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| Kafka | [`jdk8-kafka-demo`](jdk8-platform/jdk8-mq/jdk8-kafka-demo/README.md) | 8 | 基础收发 / 批量 / 分区有序 / Exactly-Once 事务 / 重试与死信 / Header 过滤。`@EmbeddedKafka` 自包含，无需外部 Broker | [Kafka 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/Kafka学习记录.md) |
| RocketMQ | [`jdk8-rocketmq-demo`](jdk8-platform/jdk8-mq/jdk8-rocketmq-demo/README.md) | 8 | 原生 Client，14 类场景：simple / batch / broadcast / delay / filter / order / pull / request-reply / retry / transaction / trace / throttle / ACL / 容错 | [RocketMQ 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/RocketMQ学习记录.md) |
| RabbitMQ | [`jdk8-rabbitmq-demo`](jdk8-platform/jdk8-mq/jdk8-rabbitmq-demo/README.md) | 8 | Spring AMQP：Exchange 类型（direct/topic/fanout/headers）、publisher confirm / consumer 手动 ack、TTL+DLX 死信与延迟消息；rabbitmq-mock 自包含 *Test + Testcontainers 真实 Broker *Test（无 Docker 优雅跳过） | — |
| MQTT | [`jdk8-mqtt-demo`](jdk8-platform/jdk8-mq/jdk8-mqtt-demo/README.md) | 8 | Eclipse Paho 客户端：发布订阅模型 / 通配符订阅（`+` 单层、`#` 多层）/ QoS 0·1·2 / 保留消息（Retained）/ 遗嘱消息（LWT）；moquette 内存 Broker 自包含 *Test + Testcontainers 真实 mosquitto *Test（无 Docker 优雅跳过） | （学习笔记待补） |

---

## 分布式与协调

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| 分布式事务 | [`jdk8-seata-demo`](jdk8-platform/jdk8-seata-demo/README.md) | 8 | Seata 1.6.x：AT（自动 undo_log 补偿）/ TCC（含空回滚、幂等、悬挂语义）/ SAGA / XA 数据源代理 / XID 跨线程传递。H2 自包含测试 | [Seata 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/Seata学习记录.md) |
| 分布式锁 | [`jdk8-redis-demo`](jdk8-platform/jdk8-redis-demo/README.md)（Redisson）、[`jdk8-zookeeper-demo`](jdk8-platform/jdk8-zookeeper-demo/README.md)（Curator） | 8 | Redisson 可重入锁 vs Curator `InterProcessMutex` 临时顺序节点锁；两者对比见模块内 WHY 注释 | [Redis 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/Redis学习记录.md) / [ZooKeeper 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/ZooKeeper学习记录.md) |
| ZooKeeper 协调 | [`jdk8-zookeeper-demo`](jdk8-platform/jdk8-zookeeper-demo/README.md) | 8 | Curator：`InterProcessMutex` 锁 / `LeaderSelector` 选主 / `NodeCache` 配置中心 + Watcher。含 docker-compose，无 ZK 时测试优雅跳过 | [ZooKeeper 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/ZooKeeper学习记录.md) |
| **多租户限流（压测量化）** | [`jdk8-ratelimiter-demo`](jdk8-platform/jdk8-ratelimiter-demo/README.md) | 8 | 三实现对照：`redis-lua`（全局精确）/ `local-redis`（本地令牌桶 + 每窗口 Redis 校准）/ `local-only`（性能下界）。压测由属性驱动，采集吞吐/延迟/Redis 负载/超限率 | **[多租户分布式限流：本地 + Redis 双层限流](https://github.com/xieqiansong/chaos-notes/blob/main/performance/多租户分布式限流-本地加Redis双层限流.md)** |
| **接口幂等** | [`jdk21-idempotent-demo`](jdk21-platform/jdk21-tech/jdk21-idempotent-demo/README.md) | 21 | 三层去重：请求级 / 消费级 / 状态机 | [接口幂等学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/接口幂等学习记录.md) |
| 数据同步（CDC） | [`jdk8-flink-cdc-sync-demo`](jdk8-platform/jdk8-tech/jdk8-flink-cdc-sync-demo/README.md) | 8 | Flink CDC 同源库表同步，覆盖 Q1–Q5。**因 Flink 1.17 不兼容 JDK 21，不能进 `jdk21-tech`** | [FlinkCDC 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/FlinkCDC学习记录.md) |

---

## 微服务与治理

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| 注册 / 配置中心 | [`jdk8-nacos-demo`](jdk8-platform/jdk8-nacos-demo/README.md) | 8 | 形态二（多进程）：拆 provider / consumer / config 三个可独立启动的进程，还原「注册 → 发现 → 跨进程调用 → 配置动态刷新」全链路 | [Nacos 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/Nacos学习记录.md) |
| 熔断限流 | [`jdk8-sentinel-demo`](jdk8-platform/jdk8-sentinel-demo/README.md) | 8 | QPS 直接/关联/WarmUp 流控、异常数/比例/慢调用比例熔断、热点参数、`@SentinelResource`、程序化 `SphU.entry()` | [Sentinel 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/Sentinel学习记录.md) |
| **热路径 Filter 异步化（压测量化）** | [`jdk8-servlet-filter-async-demo`](jdk8-platform/jdk8-servlet-filter-async-demo/README.md) | 8 | `HIGHEST_PRECEDENCE` Filter 在 DispatcherServlet 之前截断最高频接口，异步提交即返回释放 Tomcat 线程；双 mode 内嵌 Tomcat 对照，量化「绕过 MVC 省下的链路 CPU」 | **[热路径 Filter 异步化：高频接口绕过 Spring MVC](https://github.com/xieqiansong/chaos-notes/blob/main/performance/热路径Filter异步化-高频接口绕过SpringMVC.md)** |
| 微服务全链路 | [`jdk8-microservice-demo`](jdk8-platform/jdk8-microservice-demo/README.md) | 8 | 形态二（多进程，10 模块）：common 层 6 模块 + gateway/auth/user/order；已落地 P0–P6——统一响应异常、MDC traceId、多数据源（PG 主 / MySQL 副）、Sentinel 熔断降级、Seata AT 全局事务、双令牌 JWT、AccessLog 脱敏、Nacos 多环境 | [企业级微服务架构实践](https://github.com/xieqiansong/chaos-notes/blob/main/notes/企业级微服务架构实践.md) |
| **WebFlux 响应式编程** | [`jdk8-webflux-demo`](jdk8-platform/jdk8-webflux-demo/README.md) | 8 | Reactor（Mono/Flux + map/flatMap/filter）、背压（request(n) 按需拉取 + onBackpressure* 策略）、RouterFunction 函数式路由、注解式响应式 Controller（对照）、WebClient 异步非阻塞调用；Netty 非阻塞，内存仓储 + 内嵌端点，零外部依赖，StepVerifier/WebTestClient 双轨验证 | （学习笔记待补） |

---

## 数据持久层

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| MyBatis-Plus 高阶用法 | [`jdk11-mybatis-plus-demo`](jdk11-platform/jdk11-mybatis-plus-demo/README.md) | 11 | 条件构造器 / 分页（单表 + 联表）/ 逻辑删除 + 乐观锁 + 自动填充 / 多租户隔离 / 动态表名分表 / 字段透明加密（AES）。**因 `mybatis-plus-jsqlparser` 需 JDK 11+，自 `jdk8` 迁移而来**，H2 内存库零外部依赖 | [MyBatis-Plus 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/MyBatis-Plus学习记录.md) |
| 位图统计 | [`jdk8-bitmap-stat-demo`](jdk8-platform/jdk8-tech/jdk8-bitmap-stat-demo/README.md) | 8 | 位图（Bitmap）在统计场景的应用 | [位图统计学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/位图统计学习记录.md) |

---

## 搜索

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| Elasticsearch | [`jdk8-elasticsearch-demo`](jdk8-platform/jdk8-elasticsearch-demo/README.md) | 8 | ES 7.17 + Spring Data ES 4.4：索引管理 / 文档 CRUD 与批量 / match/term/range/bool 搜索 / terms/avg 聚合；`ElasticsearchRestTemplate` 与 `ElasticsearchRepository` 双轨；Testcontainers（无 Docker 优雅跳过） | [Elasticsearch 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/Elasticsearch学习记录.md) |

---

## 安全与加密

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| 安全框架与认证授权 | [`jdk8-security-demo`](jdk8-platform/jdk8-security-demo/README.md) | 8 | Spring Security 过滤器链（白名单 / Basic / 无状态）+ jjwt 签发校验 + 方法安全 + OAuth2 资源服务器（条件化启用，附 Keycloak docker-compose）；Session-Cookie 与 Token 方案对比见注释 | [Spring Security 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/SpringSecurity学习记录.md) |
| 加密与国密 | [`jdk8-crypto-demo`](jdk8-platform/jdk8-crypto-demo/README.md) | 8 | AES(CBC/GCM) / RSA（加密 + SHA256withRSA 签名）/ SHA-256 / 国密 SM2-SM3-SM4（BouncyCastle），含篡改检测断言，纯算法零外部依赖 | [加密与国密学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/加密与国密学习记录.md) |
| HMAC 鉴权 | [`jdk8-hmac-auth-demo`](jdk8-platform/jdk8-tech/jdk8-hmac-auth-demo/README.md) | 8 | HMAC 签名鉴权 | [HMAC 鉴权学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/HMAC鉴权学习记录.md) |

---

## 办公文档处理

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| Excel | [`jdk8-excel-demo`](jdk8-platform/jdk8-office-tech/jdk8-excel-demo/README.md) | 8 | POI / EasyExcel / Hutool 三体系横评，含版本守门与能力限制（HSSF 65536 行、CellStyle 上限等） | [Excel 处理学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/Excel处理学习记录.md) |
| Word | [`jdk8-word-demo`](jdk8-platform/jdk8-office-tech/jdk8-word-demo/README.md) | 8 | 文档生成与处理 | [Word 处理学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/Word处理学习记录.md) |
| PDF | [`jdk8-pdf-demo`](jdk8-platform/jdk8-office-tech/jdk8-pdf-demo/README.md) | 8 | PDFBox 处理 | [PDF 处理学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/PDF处理学习记录.md) |

---

## 工程化与测试

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| 单元测试 | [`jdk8-testing-demo`](jdk8-platform/jdk8-testing-demo/README.md) | 8 | JUnit5 + Mockito：`@Mock/@Spy/@InjectMocks`、`argThat` 与 `verify` 行为验证、BDD（given/when/then）、`@WebMvcTest` 切片测试 | [单元测试学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/单元测试学习记录.md) |
| 序列化对比 | [`jdk8-serialization-demo`](jdk8-platform/jdk8-serialization-demo/README.md) | 8 | Jackson 文本 / Kryo 二进制 / JDK 原生三方案同模型对比：往返正确性、体积、健壮性、反序列化安全坑 | [序列化学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/序列化学习记录.md) |
| 对象映射 | [`jdk8-mapstruct-demo`](jdk8-platform/jdk8-mapstruct-demo/README.md) | 8 | MapStruct：基础字段 / 集合 / 自定义方法（`@Mapping`）/ 嵌套对象，注解处理器在 `target/generated-sources` 已验证生效 | [MapStruct 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/MapStruct学习记录.md) |
| 定时任务 | [`jdk8-scheduler-demo`](jdk8-platform/jdk8-scheduler-demo/README.md) | 8 | `@Scheduled` 三种触发模型 / Quartz 内存 RAMJobStore / XXL-JOB 执行端 + 分片处理器（admin 条件化启用） | [定时任务学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/定时任务学习记录.md) |
| 自动装配机制 | [`jdk8-starter-demo`](jdk8-platform/jdk8-starter-demo/README.md) | 8 | 以零依赖的 `token-spring-boot-starter` 为载体：`AutoConfiguration.imports` 加载、`@ConfigurationProperties` 绑定、`@ConditionalOnProperty`/`@ConditionalOnMissingBean` 条件装配、第三方 starter 命名约定 | [Spring Boot Starter 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/SpringBootStarter学习记录.md) |

---

## AI 应用

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| Spring AI | [`jdk17-springai-demo`](jdk17-platform/jdk17-springai-demo/README.md) | 17 | chat / stream / memory / prompt / 结构化输出 / 工具调用 / RAG / MCP **客户端** | [Spring AI 学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/SpringAI学习记录.md) |
| MCP 服务端 | [`jdk17-mcp-server-demo`](jdk17-platform/jdk17-mcp-server-demo) | 17 | 最小 MCP 服务端（SSE 传输），供上者 MCP 客户端连接演示 | [MCP 服务学习记录](https://github.com/xieqiansong/chaos-notes/blob/main/notes/MCP服务学习记录.md) |

---

## 综合实战

> 综合实战 = 把多个技术点组合进一个带业务外壳的 demo。按 [`AGENTS.md`](AGENTS.md) 第七章，**保留但不新增**。

| 技术点 | 模块 | JDK | 关键能力 | 笔记 |
|---|---|---|---|---|
| 秒杀 | [`jdk8-seckill-demo`](jdk8-platform/jdk8-seckill-demo/README.md) | 8 | Redis 分桶库存预热 + Lua 原子扣减防超卖 + 令牌桶限流 + Redisson 锁 + Kafka 异步下单 + 库存定时回写 | [高并发秒杀实战](https://github.com/xieqiansong/chaos-notes/blob/main/notes/高并发秒杀实战.md)<br>⚠️ [秒杀链路压测与优化](https://github.com/xieqiansong/chaos-notes/blob/main/performance/秒杀链路压测与优化.md)（数据为示意框架，待实测回填） |
| 短链 | [`jdk8-short-link-demo`](jdk8-platform/jdk8-short-link-demo/README.md) | 8 | Snowflake 发号 + Base62 短码 + Redisson 布隆过滤器防穿透 + Redis 缓存 + PG 持久化 + 302 跳转 | [短链系统设计](https://github.com/xieqiansong/chaos-notes/blob/main/notes/短链系统设计.md)<br>⚠️ [短链缓存命中率与防穿透压测](https://github.com/xieqiansong/chaos-notes/blob/main/performance/短链缓存命中率与防穿透压测.md)（数据为示意框架，待实测回填） |

---

## 延伸 · 其他仓库

同一套「可运行代码 ↔ 量化笔记」方法在其他仓库的产出：

| 技术点 | 仓库 | 关键能力 | 笔记 |
|---|---|---|---|
| TCP over WebSocket 隧道 | [`chaos-tcp-over-websockets`](https://github.com/xieqiansong/chaos-tcp-over-websockets) | Netty 4.1 双架构路线（simple 直连 / multiplexed 会话多路复用）+ JMH 微基准 + 端到端压测；实测端到端吞吐 ~10→245 MB/s（simple）、~13→592 MB/s（multiplexed） | [TCP-over-WebSocket 隧道：端到端性能基准与瓶颈分析](https://github.com/xieqiansong/chaos-notes/blob/main/performance/TCP-over-WebSocket隧道-端到端性能基准与瓶颈分析.md) |

---

## 待补技术点与建设优先级

> 以上是「已有什么」，以下是「还缺什么」。重要度：**🔴 高**（核心必备）/ **🟡 中**（进阶必备）/ **⚪ 低**（专项按需）。
> 编号沿用原「Java 必学技术全景」清单，便于追溯。

### 未完成项清单

| # | 重要度 | 技术点 | 状态 | 建议落点 / 说明 |
|---|---|---|---|---|
| 16 | 🟡 中 | 字节码与 Java Agent（ASM / ByteBuddy） | 🟡 部分 | `jdk8-base` 已有 Premain/Agent-Class；缺 ASM `ClassVisitor`/`ClassWriter` 字节码操作、ByteBuddy 运行时动态代理与方法拦截。建议补 `java.agent.asm` 子包 |
| 23 | 🟡 中 | Spring Cloud Gateway 网关 | ❌ 待补 | `jdk8-gateway-demo`：路由断言工厂（Path/Host/Header）、GatewayFilter 过滤器链、集成 Sentinel 限流 + Nacos 动态路由。**依赖 Nacos + Sentinel** |
| 24 | 🟡 中 | 链路追踪（SkyWalking / Sleuth+Zipkin） | ❌ 待补 | `jdk8-tracing-demo`：SkyWalking Agent 自动探针 + MQ 跨进程追踪、Sleuth（TraceId/SpanId）+ Brave。**依赖 Nacos + MQ** |
| 31 | 🟡 中 | 监控（Actuator / Prometheus / Grafana） | ❌ 待补 | `jdk8-monitor-demo`：Actuator 端点（health/metrics/env）、Micrometer 指标暴露、Prometheus 采集 + Grafana 仪表盘、自定义业务指标。**依赖 Docker** |
| 35 | 🟡 中 | 容器与编排（Docker / Kubernetes） | 🟡 部分 | 部分 demo 已有 docker-compose；缺 Dockerfile 多阶段构建（Spring Boot 分层 JAR）、K8s Deployment/Service/ConfigMap 模板、本地 k3s/minikube 一键部署 |
| 36 | 🟡 中 | CI/CD（GitHub Actions / GitLab CI） | ❌ 待补 | **工程级**补充（非 demo 级）：`.github/workflows/build.yml`（Maven 编译 + 测试 + 打包）、`.gitlab-ci.yml` 多阶段流水线 |
| 37 | ⚪ 低 | JPA / Hibernate | ❌ 待补 | `jdk8-jpa-demo`：仓库方法命名查询、`@Query` JPQL、实体关联（`@OneToMany`/`@ManyToMany`）、二级缓存。与 MyBatis 二选一 |
| 38 | ⚪ 低 | Lucene（搜索底层） | ❌ 进阶 | 建议直接在 `jdk8-elasticsearch-demo` 内补充：倒排索引构建、IK 分词器、查询解析、高亮 |
| 41 | ⚪ 低 | ShardingSphere 分库分表 | ❌ 待补 | `jdk8-shardingsphere-demo`：水平分库分表（inline/standard 策略）、读写分离、分布式主键、绑定表 + 广播表 |

### 建设优先级

> 优先级公式：**重要程度 × 补充难度 × 依赖链**。

| 档位 | 优先级 | 补什么 | 落点 | 前置依赖 |
|---|---|---|---|---|
| 🟡 按序补（有依赖链，需独立建 demo） | P2 | Spring Cloud Gateway 网关 | `jdk8-gateway-demo` | Nacos + Sentinel |
| | P2 | SkyWalking 链路追踪 | `jdk8-tracing-demo` | Nacos + MQ |
| | P2 | Actuator + Prometheus + Grafana 监控 | `jdk8-monitor-demo` | Docker |
| ⚪ 可选补（低重要度或工程级） | P3 | K8s 部署清单（Dockerfile / K8s YAML） | 工程级补充 | — |
| | P3 | GitHub Actions + GitLab CI | 工程级补充 | — |
| | P3 | JPA / Hibernate | `jdk8-jpa-demo` | — |
| | P3 | ShardingSphere 分库分表 | `jdk8-shardingsphere-demo` | — |
| | P4 | Lucene 底层 | `jdk8-elasticsearch-demo` 内补充 | — |

> **第一档（P0/P1，高重要度、低难度、可复用现有 demo）已全部完成**：集合源码分析 + 泛型 + 注解（含 APT）、JVM GC 对比与 arthas 诊断、Mockito 与 Spring Boot 切片测试、Raft 选举与日志复制、ZooKeeper、Spring Security + JWT + OAuth2、加密与国密、序列化、定时任务、多级缓存。

> **新建 demo 的模板**：一律参照 ★ 标杆 [`jdk8-localcache-demo`](jdk8-platform/jdk8-localcache-demo/README.md)，并遵循 [`AGENTS.md`](AGENTS.md) 的「AI 生成自检清单」。

---

## 维护说明

- 新增模块后请同步更新：本文件对应技术域的表 + 根 [`README.md`](README.md) 的模块树 + 所在平台 `README.md`。
- 补完某个待补项后，把它从「未完成项清单」移入上方对应技术域，并从「建设优先级」表删除。
- Demo 的形态、命名、测试与 README 规范见 [`AGENTS.md`](AGENTS.md)。
