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
| 1 | `jdk8-base` | ✅ 完成 | Java 基础内功：分布式 ID/Paxos、IO/NIO/Netty/SPI/JUC 并发、JVM/类加载/迷你 Servlet 容器/JMH/Java Agent 字节码增强（106 个可运行小例子） |
| 2 | `jdk8-redis-demo` | ✅ 完成 | Redis 全场景：缓存、Hash/List/Set、ZSet 排行榜、计数、分布式锁、Lua 限流/扣库存、Pipeline、PubSub |
| 3 | `jdk8-mybatis-plus-demo` | ⚪ 未开始 | 已建模块骨架，待补充 MyBatis-Plus 示例（持久层 ORM，几乎所有 Java 项目必备） |
| 4 | `jdk8-nacos-demo` | ✅ 完成 | Nacos 服务注册发现 + 配置中心 |
| 5 | `jdk8-rocketmq-demo` | ✅ 完成 | RocketMQ 全场景：simple / batch / broadcast / delay / filter / order / pull / requestreply / retry / transaction / trace / throttle / acl / faulttolerant |
| 6 | `jdk8-kafka-demo` | ✅ 完成 | Kafka 全场景：基础收发、批量、分区有序、Exactly-Once 事务、重试/死信、Header 过滤 |
| 7 | `jdk8-sentinel-demo` | ✅ 完成 | Sentinel 流控/熔断/热点参数/@SentinelResource |
| 8 | `jdk8-seata-demo` | ✅ 完成 | Seata 分布式事务：AT 自动补偿 + TCC |
| 9 | `jdk8-elasticsearch-demo` | ✅ 完成 | Elasticsearch 索引/文档/搜索/聚合 |
| 10 | `jdk8-mapstruct-demo` | ✅ 完成 | MapStruct 对象映射：basic / collection / custom / nested |
| 11 | `jdk8-seckill-demo` | ✅ 完成 | 秒杀综合实战：Redis 分桶库存 + Lua 扣减 + Redisson 锁 + Kafka 异步下单 + 令牌桶限流 |
| 12 | `jdk8-short-link-demo` | ✅ 完成 | 短链综合实战：Snowflake+Base62 短码 + 布隆过滤器防穿透 + Redis 缓存 + PG 持久化 |
| 13 | `jdk8-common` | 🟡 占位 | 公共基础模块占位，承载跨 demo 的公共工具/实体 |

## 模块详情（同按重要程度排序）

- **jdk8-base**：Java 基础与杂项知识点合集，由 old 四份模块（`java-guide-demos` + `tech-pdai-java-demos` + `chaos-test` + `javaagent`）合并而来，共 106 个可运行小例子。覆盖分布式 ID（UUID/Snowflake/Redis-INCR/Leaf/TinyId）、最小 Basic Paxos、反射/异常/SPI、BIO/NIO/AIO/Netty/零拷贝/Reactor 多路复用、JUC 并发全家桶（AQS/锁/阻塞队列/Fork-Join/线程池/Phaser）、JVM 类加载/CGLib 代理、手写迷你 Servlet 容器、JMH 微基准、Java Agent（Premain/Agent-Class 字节码增强）。包名保持原样，无 Web 外壳，多数类自带 `main` 直接运行。
- **jdk8-redis-demo**：基于 Spring Data Redis + Lettuce，按能力分包（cache/collection/rank/counter/lock/ratelimit/stock/pipeline/pubsub），以单元测试为核心验证手段。
- **jdk8-mybatis-plus-demo**：已建模块骨架，待补充 MyBatis-Plus 示例（CRUD、条件构造器、分页、逻辑删除等），面向持久层 ORM 这一几乎所有 Java 项目都需要的环节。
- **jdk8-nacos-demo**：基于 Spring Cloud Alibaba，演示 Provider 注册、`@LoadBalanced` RestTemplate / OpenFeign 消费、配置中心 `@RefreshScope` 动态刷新与编程式 `ConfigService.addListener` 监听。
- **jdk8-rocketmq-demo**：使用原生 RocketMQ Client，以独立类演示各类消息模型与生产消费模式，并包含幂等、重试、死信、事务、ACL、故障容错、消息轨迹等进阶能力。
- **jdk8-kafka-demo**：基于 Spring Kafka 2.8.x，覆盖基础收发、批量发送/消费、分区有序、Exactly-Once 事务、重试/死信、Header 消息过滤，@EmbeddedKafka 自包含测试无需外部 Broker。
- **jdk8-sentinel-demo**：基于 Sentinel 1.8.6 + Spring Cloud Alibaba，覆盖 QPS 直接/关联/WarmUp 流控、异常数/异常比例/慢调用比例熔断、热点参数限流、@SentinelResource 注解（blockHandler/fallback），SphU.entry() 程序化方式核心稳定，包含 Dashboard docker-compose。
- **jdk8-seata-demo**：基于 Seata 1.6.x + Spring Cloud Alibaba，覆盖 AT 模式（自动 undo_log 补偿）和 TCC 模式（手动 Try/Confirm/Cancel），JdbcTemplate 直操作业聚焦事务概念，H2 内存数据库自包含测试。
- **jdk8-elasticsearch-demo**：基于 Elasticsearch 7.17 + Spring Data Elasticsearch 4.4，覆盖索引管理、文档 CRUD/批量、match/term/range/bool 搜索、terms/avg 聚合，`ElasticsearchRestTemplate` 与 `ElasticsearchRepository` 双轨，Testcontainers 集成测试（无 Docker 优雅跳过）。
- **jdk8-mapstruct-demo**：覆盖基础字段映射、集合映射、自定义方法（`@Mapping` + `MappingUtil`）、嵌套对象映射；`target/generated-sources` 下已生成 `XxxMapperImpl` 验证注解处理器生效，测试覆盖 basic / collection / custom / nested。
- **jdk8-seckill-demo**：基于 Spring Boot 2.7 + Redis/Redisson/PostgreSQL/Kafka，覆盖秒杀全链路——分桶库存预热、Lua 原子扣减防超卖、令牌桶限流、Redisson 分布式锁、Kafka 异步下单、库存定时回写，含 docker-compose 一键起依赖。
- **jdk8-short-link-demo**：基于 Spring Boot 2.7 + Redis/Redisson/PostgreSQL，覆盖短链全链路——Snowflake 发号 + Base62 编码短码、Redisson 布隆过滤器防缓存穿透、Redis 缓存 + PG 持久化、302 跳转，含 docker-compose 一键起依赖。
- **jdk8-common**：公共基础模块占位（`App.java`），后续承载跨 demo 的公共工具/实体。

## 备注与待办

- `jdk8-mybatis-plus-demo` 仅骨架，待补充示例。
- `tech-pdai-spring-demos`（old 仓库）体量大（1000+ 文件），按需单独拆分为 `jdk8-*` 子模块迁移，不在本次聚合范围内。
- `jdk8-common` 为占位模块，暂未承载实际内容。
