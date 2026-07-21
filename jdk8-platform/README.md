# jdk8-platform

> 基于 **JDK 8 + Spring Boot 2.7.18** 的技术学习聚合工程。所有示例统一以 `jdk8-*-demo` 形式作为子模块，集中管理依赖版本（Spring Cloud / Alibaba、MyBatis-Plus、Hutool、Guava、Lombok 等），避免每个 demo 重复配置。

## 技术栈

| 分类 | 版本 |
|------|------|
| JDK | 1.8（maven.compiler.source/target = 1.8） |
| Spring Boot | 2.7.18 |
| Spring Cloud | 2021.0.9 |
| Spring Cloud Alibaba | 2021.0.6.2 |
| MyBatis-Plus | 3.5.17 |
| Druid | 1.2.23 |
| Hutool | 5.8.46 |
| Guava | 33.6.0-jre |
| Lombok | 1.18.46 |

## 模块总览（按 Java 开发重要程度排序）

> 排序思路：① Java 内核与最高频基础设施（并发、IO、缓存、持久层）→ ② 分布式 / 微服务核心中间件 → ③ 专项能力 → ④ 综合实战 → ⑤ 占位。

| 重要度 | 模块 | 状态 | 说明 |
|--------|------|------|------|
| 1 | `jdk8-base` | ✅ 完成 | Java 基础内功：分布式 ID/Paxos、IO/NIO/Netty/SPI/JUC 并发、JVM/类加载/迷你 Servlet 容器/JMH/Java Agent 字节码增强；JDK8 新特性：Lambda/Stream/Optional/方法引用/默认方法/日期时间/Base64/StringJoiner/CompletableFuture（106 个可运行小例子+新特性场景） |
| 2 | `jdk8-redis-demo` | ✅ 完成 | Redis 全场景：缓存、Hash/List/Set、ZSet 排行榜、计数、分布式锁、Lua 限流/扣库存、Pipeline、PubSub |
| 3 | `jdk8-localcache-demo` | ✅ 完成 ★ 标杆 | 本地缓存（Caffeine）：基础读写 / 写入过期 / 容量淘汰 / 声明式 @Cacheable，纯内存零外部依赖。**后续 A 类 demo 模板** |
| 4 | `jdk8-mybatis-plus-demo` | ✅ 完成 | MyBatis-Plus 高阶：条件构造器 / 分页（单表+联表）/ 逻辑删除+乐观锁+自动填充 / 多租户隔离 / 动态表名分表 / 字段透明加密（AES），以单元测试为核心验证，H2 内存库零外部依赖 |
| 5 | `jdk8-testing-demo` | ✅ 完成 | 单元测试专项：JUnit5 + Mockito `@Mock/@Spy/@InjectMocks`、参数匹配与行为验证（`argThat`/`verify`）、BDD 风格（given/when/then）、Spring Boot 切片测试（`@WebMvcTest`），各场景均含可断言 `*Test` |
| 6 | `jdk8-nacos-demo` | ✅ 完成 | Nacos 服务注册发现 + 配置中心 |
| 7 | `jdk8-rocketmq-demo` | ✅ 完成 | RocketMQ 全场景：simple / batch / broadcast / delay / filter / order / pull / requestreply / retry / transaction / trace / throttle / acl / faulttolerant |
| 8 | `jdk8-kafka-demo` | ✅ 完成 | Kafka 全场景：基础收发、批量、分区有序、Exactly-Once 事务、重试/死信、Header 过滤 |
| 9 | `jdk8-sentinel-demo` | ✅ 完成 | Sentinel 流控/熔断/热点参数/@SentinelResource |
| 10 | `jdk8-seata-demo` | ✅ 完成 | Seata 分布式事务：AT 自动补偿 + TCC |
| 11 | `jdk8-elasticsearch-demo` | ✅ 完成 | Elasticsearch 索引/文档/搜索/聚合 |
| 12 | `jdk8-zookeeper-demo` | ✅ 完成 | ZooKeeper 协调：Curator 客户端、InterProcessMutex 临时顺序节点分布式锁、LeaderSelector 选主、NodeCache 配置中心 + Watcher 监听，含 docker-compose，无 ZK 时测试优雅跳过 |
| 13 | `jdk8-security-demo` | ✅ 完成 | Spring Security 过滤器链（白名单/Basic/无状态）+ jjwt 签发校验 JWT 过滤器 + OAuth2 资源服务器（条件化启用，附 Keycloak docker-compose） |
| 14 | `jdk8-crypto-demo` | ✅ 完成 | 加密与签名：AES(CBC/GCM)/RSA(加密+SHA256withRSA 签名)/SHA-256/国密 SM2-SM3-SM4（BouncyCastle），含篡改检测断言 |
| 15 | `jdk8-serialization-demo` | ✅ 完成 | 序列化对比：Jackson 文本 / Kryo 二进制 / JDK 原生三方案同模型对比（往返正确性、体积、健壮性、反序列化安全坑） |
| 16 | `jdk8-scheduler-demo` | ✅ 完成 | 定时任务：@Scheduled 三种触发模型 / Quartz 内存 RAMJobStore / XXL-JOB 执行端+分片处理器（admin 条件化启用） |
| 17 | `jdk8-mapstruct-demo` | ✅ 完成 | MapStruct 对象映射：basic / collection / custom / nested |
| 18 | `jdk8-seckill-demo` | ✅ 完成 | 秒杀综合实战：Redis 分桶库存 + Lua 扣减 + Redisson 锁 + Kafka 异步下单 + 令牌桶限流 |
| 19 | `jdk8-short-link-demo` | ✅ 完成 | 短链综合实战：Snowflake+Base62 短码 + 布隆过滤器防穿透 + Redis 缓存 + PG 持久化 |
| 20 | `jdk8-common` | 🟡 占位 | 公共基础模块占位，承载跨 demo 的公共工具/实体 |

## 模块详情（同按重要程度排序）

- **jdk8-base**：Java 基础与杂项知识点合集，由 old 四份模块 + `jdk8-base-features-demo` 合并而来，共 106 个可运行小例子 + JDK8 新特性场景。覆盖分布式 ID（UUID/Snowflake/Redis-INCR/Leaf/TinyId）、最小 Basic Paxos、反射/异常/SPI、BIO/NIO/AIO/Netty/零拷贝/Reactor 多路复用、JUC 并发全家桶（AQS/锁/阻塞队列/Fork-Join/线程池/Phaser）、JVM 类加载/CGLib 代理、手写迷你 Servlet 容器、JMH 微基准、Java Agent（Premain/Agent-Class 字节码增强）；JDK8 新特性：Lambda/Stream/Optional/方法引用/默认方法/日期时间 API（LocalDate/LocalTime/Instant/ZoneId）、Base64、StringJoiner、CompletableFuture 异步编排。包名保持原样，无 Web 外壳，多数类自带 `main` 或单元测试直接运行。
- **jdk8-redis-demo**：基于 Spring Data Redis + Lettuce，按能力分包（cache/collection/rank/counter/lock/ratelimit/stock/pipeline/pubsub），以单元测试为核心验证手段。
- **jdk8-localcache-demo** ★ 标杆模板：基于 Spring Boot 2.7 + Caffeine 2.9.3，纯内存零外部依赖；按能力分包（basic 基础读写 / expire 写入过期 / eviction 容量淘汰 / cacheaside 声明式 @Cacheable），以单元测试为核心验证，控制台入口 `DemoApp.main` 一键打印各场景「输入 → 输出」。**后续新增 A 类 demo 均以它为模板**（见根 `AGENTS.md`）。
- **jdk8-mybatis-plus-demo**：基于 Spring Boot 2.7 + MyBatis-Plus 3.5.16 + H2 内存库，按能力分包（wrapper / page / audit / tenant / dynamictable / encrypt），以单元测试为核心验证手段，控制台入口 `DemoApp.main` 可一键打印各场景「输入 → 输出」。
- **jdk8-nacos-demo**：基于 Spring Cloud Alibaba，演示 Provider 注册、`@LoadBalanced` RestTemplate / OpenFeign 消费、配置中心 `@RefreshScope` 动态刷新与编程式 `ConfigService.addListener` 监听。采用**形态二（多模块）**：Nacos 的价值在于「服务注册 → 被发现 → 跨进程调用 → 配置动态刷新」的完整链路，按 provider / consumer / config 拆成可各自独立启动的进程，才能真实还原这一过程（单模块模拟不出跨进程效果）。
- **jdk8-rocketmq-demo**：使用原生 RocketMQ Client，以独立类演示各类消息模型与生产消费模式，并包含幂等、重试、死信、事务、ACL、故障容错、消息轨迹等进阶能力。
- **jdk8-kafka-demo**：基于 Spring Kafka 2.8.x，覆盖基础收发、批量发送/消费、分区有序、Exactly-Once 事务、重试/死信、Header 消息过滤，@EmbeddedKafka 自包含测试无需外部 Broker。
- **jdk8-sentinel-demo**：基于 Sentinel 1.8.6 + Spring Cloud Alibaba，覆盖 QPS 直接/关联/WarmUp 流控、异常数/异常比例/慢调用比例熔断、热点参数限流、@SentinelResource 注解（blockHandler/fallback），SphU.entry() 程序化方式核心稳定，包含 Dashboard docker-compose。
- **jdk8-seata-demo**：基于 Seata 1.6.x + Spring Cloud Alibaba，覆盖 AT 模式（自动 undo_log 补偿）和 TCC 模式（手动 Try/Confirm/Cancel），JdbcTemplate 直操作业聚焦事务概念，H2 内存数据库自包含测试。
- **jdk8-elasticsearch-demo**：基于 Elasticsearch 7.17 + Spring Data Elasticsearch 4.4，覆盖索引管理、文档 CRUD/批量、match/term/range/bool 搜索、terms/avg 聚合，`ElasticsearchRestTemplate` 与 `ElasticsearchRepository` 双轨，Testcontainers 集成测试（无 Docker 优雅跳过）。
- **jdk8-mapstruct-demo**：覆盖基础字段映射、集合映射、自定义方法（`@Mapping` + `MappingUtil`）、嵌套对象映射；`target/generated-sources` 下已生成 `XxxMapperImpl` 验证注解处理器生效，测试覆盖 basic / collection / custom / nested。
- **jdk8-seckill-demo**：基于 Spring Boot 2.7 + Redis/Redisson/PostgreSQL/Kafka，覆盖秒杀全链路——分桶库存预热、Lua 原子扣减防超卖、令牌桶限流、Redisson 分布式锁、Kafka 异步下单、库存定时回写，含 docker-compose 一键起依赖。
- **jdk8-short-link-demo**：基于 Spring Boot 2.7 + Redis/Redisson/PostgreSQL，覆盖短链全链路——Snowflake 发号 + Base62 编码短码、Redisson 布隆过滤器防缓存穿透、Redis 缓存 + PG 持久化、302 跳转，含 docker-compose 一键起依赖。
- **jdk8-testing-demo**：基于 JUnit5 + Mockito，覆盖 `@Mock/@Spy/@InjectMocks` 注入、`argThat` 参数匹配与 `verify` 行为验证、BDD 风格（given/when/then）、Spring Boot 切片测试（`@WebMvcTest`），各场景均含可断言 `*Test`，纯库零外部依赖。
- **jdk8-zookeeper-demo**：基于 Curator，覆盖 InterProcessMutex 临时顺序节点分布式锁（Redis vs ZK 锁对比见 WHY 注释）、LeaderSelector 选主、NodeCache 配置中心 + Watcher 监听，含 docker-compose，无 ZK 时测试经 `Assumptions` 优雅跳过。
- **jdk8-security-demo**：基于 Spring Security + jjwt，覆盖过滤器链（白名单/Basic/无状态）、JWT 签发校验过滤器、方法安全；OAuth2 资源服务器条件化启用（附 Keycloak docker-compose），Session-Cookie 与 Token 方案对比见注释。
- **jdk8-crypto-demo**：基于 JDK 原生 + BouncyCastle，覆盖 AES(CBC/GCM)/RSA(加密+SHA256withRSA 签名)/SHA-256/国密 SM2-SM3-SM4，含篡改检测断言，纯算法零外部依赖。
- **jdk8-serialization-demo**：覆盖 Jackson 文本 / Kryo 二进制 / JDK 原生三方案同模型对比（往返正确性、体积、健壮性、反序列化安全坑），各场景含可断言 `*Test`。
- **jdk8-scheduler-demo**：覆盖 @Scheduled 三种触发模型（cron/fixedRate/fixedDelay）/ Quartz 内存 RAMJobStore / XXL-JOB 执行端+分片处理器（admin 条件化启用），含 docker-compose。
- **jdk8-common**：公共基础模块占位（`App.java`），后续承载跨 demo 的公共工具/实体。

## 备注与待办

- `tech-pdai-spring-demos`（old 仓库）体量大（1000+ 文件），按需单独拆分为 `jdk8-*` 子模块迁移，不在本次聚合范围内。
- `jdk8-common` 为占位模块，暂未承载实际内容。
- **★ 标杆 demo（新 demo 必须照此）**：`jdk8-localcache-demo`（本地缓存）。后续新增 A 类 demo 的目录结构、注释风格、测试形态、README 七段式，均以它为模板，详见根目录 `AGENTS.md` 的「AI 生成自检清单」。
- **补充计划**：详见独立文档 [`JAVA-TECH-PANORAMA.md`](./JAVA-TECH-PANORAMA.md) 的「补充计划与优先级」章节，当前待补的 🟡 部分覆盖项和 ❌ 待补项均已规划具体 demo 和建设顺序。

## 扩展阅读

- **Java 必学技术全景（对照本工程）**：独立文档 [`JAVA-TECH-PANORAMA.md`](./JAVA-TECH-PANORAMA.md)，含推荐学习路线、分类明细、补充计划与优先级。
