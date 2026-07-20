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
| 3 | `jdk8-localcache-demo` | ✅ 完成 ★ 标杆 | 本地缓存（Caffeine）：基础读写 / 写入过期 / 容量淘汰 / 声明式 @Cacheable，纯内存零外部依赖。**后续 A 类 demo 模板** |
| 4 | `jdk8-mybatis-plus-demo` | ✅ 完成 | MyBatis-Plus 高阶：条件构造器 / 分页（单表+联表）/ 逻辑删除+乐观锁+自动填充 / 多租户隔离 / 动态表名分表 / 字段透明加密（AES），以单元测试为核心验证，H2 内存库零外部依赖 |
| 5 | `jdk8-nacos-demo` | ✅ 完成 | Nacos 服务注册发现 + 配置中心 |
| 6 | `jdk8-rocketmq-demo` | ✅ 完成 | RocketMQ 全场景：simple / batch / broadcast / delay / filter / order / pull / requestreply / retry / transaction / trace / throttle / acl / faulttolerant |
| 7 | `jdk8-kafka-demo` | ✅ 完成 | Kafka 全场景：基础收发、批量、分区有序、Exactly-Once 事务、重试/死信、Header 过滤 |
| 8 | `jdk8-sentinel-demo` | ✅ 完成 | Sentinel 流控/熔断/热点参数/@SentinelResource |
| 9 | `jdk8-seata-demo` | ✅ 完成 | Seata 分布式事务：AT 自动补偿 + TCC |
| 10 | `jdk8-elasticsearch-demo` | ✅ 完成 | Elasticsearch 索引/文档/搜索/聚合 |
| 11 | `jdk8-mapstruct-demo` | ✅ 完成 | MapStruct 对象映射：basic / collection / custom / nested |
| 12 | `jdk8-seckill-demo` | ✅ 完成 | 秒杀综合实战：Redis 分桶库存 + Lua 扣减 + Redisson 锁 + Kafka 异步下单 + 令牌桶限流 |
| 13 | `jdk8-short-link-demo` | ✅ 完成 | 短链综合实战：Snowflake+Base62 短码 + 布隆过滤器防穿透 + Redis 缓存 + PG 持久化 |
| 14 | `jdk8-common` | 🟡 占位 | 公共基础模块占位，承载跨 demo 的公共工具/实体 |

## 模块详情（同按重要程度排序）

- **jdk8-base**：Java 基础与杂项知识点合集，由 old 四份模块（`java-guide-demos` + `tech-pdai-java-demos` + `chaos-test` + `javaagent`）合并而来，共 106 个可运行小例子。覆盖分布式 ID（UUID/Snowflake/Redis-INCR/Leaf/TinyId）、最小 Basic Paxos、反射/异常/SPI、BIO/NIO/AIO/Netty/零拷贝/Reactor 多路复用、JUC 并发全家桶（AQS/锁/阻塞队列/Fork-Join/线程池/Phaser）、JVM 类加载/CGLib 代理、手写迷你 Servlet 容器、JMH 微基准、Java Agent（Premain/Agent-Class 字节码增强）。包名保持原样，无 Web 外壳，多数类自带 `main` 直接运行。
- **jdk8-redis-demo**：基于 Spring Data Redis + Lettuce，按能力分包（cache/collection/rank/counter/lock/ratelimit/stock/pipeline/pubsub），以单元测试为核心验证手段。
- **jdk8-localcache-demo** ★ 标杆模板：基于 Spring Boot 2.7 + Caffeine 2.9.3，纯内存零外部依赖；按能力分包（basic 基础读写 / expire 写入过期 / eviction 容量淘汰 / cacheaside 声明式 @Cacheable），以单元测试为核心验证，控制台入口 `DemoApp.main` 一键打印各场景「输入 → 输出」。**后续新增 A 类 demo 均以它为模板**（见根 `AGENTS.md`）。
- **jdk8-mybatis-plus-demo**：基于 Spring Boot 2.7 + MyBatis-Plus 3.5.3.2 + H2 内存库，按能力分包（wrapper / page / audit / tenant / dynamictable / encrypt），以单元测试为核心验证手段，控制台入口 `DemoApp.main` 可一键打印各场景「输入 → 输出」。
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

- `tech-pdai-spring-demos`（old 仓库）体量大（1000+ 文件），按需单独拆分为 `jdk8-*` 子模块迁移，不在本次聚合范围内。
- `jdk8-common` 为占位模块，暂未承载实际内容。
- **★ 标杆 demo（新 demo 必须照此）**：`jdk8-localcache-demo`（本地缓存）。后续新增 A 类 demo 的目录结构、注释风格、测试形态、README 七段式，均以它为模板，详见根目录 `AGENTS.md` 的「AI 生成自检清单」。

## Java 必学技术全景（对照本工程）

按「Java 后端开发核心能力」梳理的必学技术，并对照本工程是否已提供 demo：**✅ 已有** / **🟡 部分覆盖** / **❌ 待补**；每项标注重要程度 **🔴 高**（核心必备）/ **🟡 中**（进阶必备）/ **⚪ 低**（专项按需）。

### 推荐学习路线（按重要程度排序）

> 同重要度内按「先打地基 → 再高频中间件 → 后专项」的逻辑排列；✅ 表示本工程已有 demo 可直接对照学，❌/🟡 表示待补，建议学完前面的再补对应 demo。

| # | 重要度 | 技术 | 状态 | 对应模块 / 建议 |
|---|--------|------|------|----------------|
| 1 | 🔴 高 | Java 集合 / 泛型 / 异常 / 反射 / 注解 | 🟡 部分 | `jdk8-base`（反射/异常/SPI 示例） |
| 2 | 🔴 高 | 并发编程 JUC（线程池 / 锁 / AQS / 并发容器） | ✅ | `jdk8-base` |
| 3 | 🔴 高 | JVM（内存模型 / 类加载 / GC） | 🟡 部分 | `jdk8-base`（类加载/CGLib/HeapOOM，缺调优实战） |
| 4 | 🔴 高 | I/O 与网络（BIO / NIO / Netty / 零拷贝 / Reactor） | ✅ | `jdk8-base` |
| 5 | 🔴 高 | Spring / Spring Boot（IOC / AOP / 自动装配） | ✅ | 全工程基础 |
| 6 | 🔴 高 | Servlet / Filter / Listener（手写容器） | ✅ | `jdk8-base` 迷你 Servlet 容器 |
| 7 | 🔴 高 | MyBatis / MyBatis-Plus（ORM 持久层） | ✅ | `jdk8-mybatis-plus-demo`（条件构造器/分页/逻辑删除+乐观锁/多租户/动态表名/字段加密） |
| 8 | 🔴 高 | Redis（缓存 / 分布式锁 / Lua） | ✅ | `jdk8-redis-demo` |
| 9 | 🔴 高 | 单元测试（JUnit5 / Mockito） | 🟡 部分 | 各 demo 测试，建议系统补 |
| 10 | 🔴 高 | 注册 / 配置中心（Nacos） | ✅ | `jdk8-nacos-demo` |
| 11 | 🔴 高 | 熔断限流（Sentinel） | ✅ | `jdk8-sentinel-demo` |
| 12 | 🔴 高 | 分布式锁（Redis / ZooKeeper） | 🟡 部分 | `jdk8-redis-demo` / `jdk8-seckill-demo` |
| 13 | 🔴 高 | 分布式 ID（Snowflake / Leaf / TinyId） | ✅ | `jdk8-base` |
| 14 | 🔴 高 | 构建工具（Maven） | ✅ | 本工程 |
| 15 | 🟡 中 | Java 8+ 新特性（Lambda / Stream / Optional） | ❌ 待补 | 建议补 demo |
| 16 | 🟡 中 | 字节码与 Java Agent（ASM / ByteBuddy） | 🟡 部分 | `jdk8-base`（Premain/Agent-Class，缺 ASM） |
| 17 | 🟡 中 | Spring MVC / WebFlux（响应式） | 🟡 / ❌ | MVC 间接覆盖；WebFlux 待补 |
| 18 | 🟡 中 | 数据库连接池（Druid / HikariCP） | ✅ | 依赖已引入 |
| 19 | 🟡 中 | 本地缓存（Caffeine / Guava Cache） | ✅ | `jdk8-localcache-demo`（★ 后续 A 类 demo 模板） |
| 20 | 🟡 中 | 消息队列 RocketMQ | ✅ | `jdk8-rocketmq-demo` |
| 21 | 🟡 中 | 消息队列 Kafka | ✅ | `jdk8-kafka-demo` |
| 22 | 🟡 中 | 分布式事务（Seata AT/TCC） | ✅ | `jdk8-seata-demo` |
| 23 | 🟡 中 | 网关（Spring Cloud Gateway） | ❌ 待补 | 建议补 demo |
| 24 | 🟡 中 | 链路追踪（SkyWalking / Sleuth+Zipkin） | ❌ 待补 | 建议补 demo |
| 25 | 🟡 中 | ZooKeeper（协调 / 选主 / 配置） | ❌ 待补 | 建议补 demo |
| 26 | 🟡 中 | 一致性（Paxos / Raft / ZAB） | 🟡 部分 | `jdk8-base` 最小 Basic Paxos |
| 27 | 🟡 中 | 安全框架（Spring Security / Shiro） | ❌ 待补 | 建议补 demo |
| 28 | 🟡 中 | 认证授权（OAuth2 / JWT / Session） | ❌ 待补 | 建议补 demo |
| 29 | 🟡 中 | 加密与签名（AES / RSA / 国密） | ❌ 待补 | 建议补 demo |
| 30 | 🟡 中 | 序列化（JSON / Protobuf / Kryo） | ❌ 待补 | 建议补 demo |
| 31 | 🟡 中 | 监控（Actuator / Prometheus / Grafana） | ❌ 待补 | 建议补 demo |
| 32 | 🟡 中 | 定时任务（XXL-JOB / Quartz / Scheduling） | ❌ 待补 | 建议补 demo |
| 33 | 🟡 中 | 对象映射（MapStruct） | ✅ | `jdk8-mapstruct-demo` |
| 34 | 🟡 中 | 搜索引擎（Elasticsearch） | ✅ | `jdk8-elasticsearch-demo` |
| 35 | 🟡 中 | 容器与编排（Docker / Kubernetes） | 🟡 部分 | 部分 demo 提供 docker-compose |
| 36 | 🟡 中 | CI/CD（GitLab CI / GitHub Actions） | ❌ 待补 | 建议补 |
| 37 | ⚪ 低 | JPA / Hibernate（ORM 另一选型） | ❌ 待补 | 与 MyBatis 二选一 |
| 38 | ⚪ 低 | Lucene（搜索底层） | ❌ 进阶 | 进阶了解 |
| 39 | ⚪ 低 | 多级缓存（本地 + Redis） | ❌ 待补 | 进阶实战 |
| 40 | ⚪ 低 | RabbitMQ（消息队列另一选型） | ❌ 待补 | 与 RocketMQ/Kafka 二选一 |
| 41 | ⚪ 低 | 分库分表（ShardingSphere） | ❌ 待补 | 海量数据专项 |

### 分类明细（含重要度）

#### 一、Java 语言与内核（根基）
- 🔴 高 集合 / 泛型 / 异常 / 反射 / 注解 — 🟡 部分（`jdk8-base` 含反射/异常/SPI 示例）
- 🔴 高 并发编程 JUC（线程池 / 锁 / AQS / 并发容器 / Fork-Join / Phaser） — ✅ `jdk8-base`
- 🔴 高 JVM（内存模型 / 类加载 / GC / 调优工具 arthas·jstack·jmap） — 🟡 部分（`jdk8-base` 含类加载/CGLib/HeapOOM，缺调优实战）
- 🔴 高 I/O 与网络（BIO / NIO / AIO / Netty / 零拷贝 / Reactor） — ✅ `jdk8-base`
- 🟡 中 Java 8+ 新特性（Lambda / Stream / Optional / 日期 API） — ❌ 建议补
- 🟡 中 字节码与 Java Agent（instrument / ASM / ByteBuddy） — 🟡 部分（`jdk8-base` 含 Premain/Agent-Class，缺 ASM/ByteBuddy）

#### 二、Web 与框架
- 🔴 高 Servlet / Filter / Listener / 手写容器 — ✅ `jdk8-base` 迷你 Servlet 容器
- 🔴 高 Spring / Spring Boot（IOC / AOP / 自动装配） — ✅ 全工程基础
- 🟡 中 Spring MVC / **WebFlux（响应式）** — 🟡 MVC 间接覆盖 / ❌ WebFlux 待补
- 🔴 高 MyBatis / MyBatis-Plus（ORM） — ✅ `jdk8-mybatis-plus-demo`（条件构造器/分页/逻辑删除+乐观锁/多租户/动态表名/字段加密）
- ⚪ 低 JPA / Hibernate（ORM） — ❌ 待补

#### 三、数据持久层与缓存
- 🔴 高 数据库连接池（Druid / HikariCP） — ✅ 依赖已引入
- 🔴 高 Redis（缓存 / 分布式锁 / Lua） — ✅ `jdk8-redis-demo`
- 🟡 中 本地缓存（Caffeine / Guava Cache / Ehcache） — ✅ `jdk8-localcache-demo`（★ 后续 A 类 demo 模板）
- ⚪ 低 多级缓存（本地 + Redis） — ❌ 待补

#### 四、消息队列
- 🟡 中 RocketMQ — ✅ `jdk8-rocketmq-demo`
- 🟡 中 Kafka — ✅ `jdk8-kafka-demo`
- ⚪ 低 RabbitMQ — ❌ 待补

#### 五、搜索
- 🟡 中 Elasticsearch — ✅ `jdk8-elasticsearch-demo`
- ⚪ 低 Lucene（底层） — ❌ 进阶

#### 六、微服务
- 🔴 高 注册 / 配置中心（Nacos） — ✅ `jdk8-nacos-demo`
- 🔴 高 服务调用与负载均衡（OpenFeign / LoadBalancer） — ✅ `jdk8-nacos-demo`
- 🔴 高 熔断限流（Sentinel） — ✅ `jdk8-sentinel-demo`
- 🔴 高 分布式事务（Seata AT/TCC） — ✅ `jdk8-seata-demo`
- 🟡 中 网关（Spring Cloud Gateway） — ❌ 待补
- 🟡 中 链路追踪（SkyWalking / Sleuth+Zipkin） — ❌ 待补

#### 七、分布式与协调
- 🔴 高 分布式 ID（Snowflake / Leaf / TinyId / Redis） — ✅ `jdk8-base`
- 🔴 高 分布式锁（Redis / ZooKeeper） — 🟡 Redis 部分（`jdk8-redis-demo` / `jdk8-seckill-demo`）
- 🟡 中 ZooKeeper（协调 / 选主 / 配置） — ❌ 待补
- 🟡 中 一致性（Paxos / Raft / ZAB） — 🟡 部分（`jdk8-base` 最小 Basic Paxos）
- ⚪ 低 分库分表（ShardingSphere） — ❌ 待补

#### 八、安全
- 🟡 中 Spring Security / Shiro — ❌ 待补
- 🟡 中 认证授权（OAuth2 / JWT / Session） — ❌ 待补
- 🟡 中 加密与签名（AES / RSA / 国密） — ❌ 待补

#### 九、工具、测试与序列化
- 🔴 高 单元测试（JUnit5 / Mockito） — 🟡 部分（`mapstruct`/`redis` 等已写测试，可更系统）
- 🟡 中 对象映射（MapStruct） — ✅ `jdk8-mapstruct-demo`
- 🟡 中 序列化（JSON / Protobuf / Kryo / Hessian） — ❌ 待补
- 🟡 中 定时任务（XXL-JOB / Quartz / Scheduling） — ❌ 待补

#### 十、工程化与运维
- 🔴 高 构建（Maven / Gradle） — ✅ 本工程
- 🟡 中 容器与编排（Docker / Kubernetes） — 🟡 部分 demo 提供 docker-compose
- 🟡 中 监控（Actuator / Prometheus / Grafana） — ❌ 待补
- 🟡 中 CI/CD（GitLab CI / GitHub Actions） — ❌ 待补

