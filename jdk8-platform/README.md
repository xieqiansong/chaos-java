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
- **补充计划**：参见下方「Java 必学技术全景」→「补充计划与优先级」章节，当前待补的 🟡 部分覆盖项和 ❌ 待补项均已规划具体 demo 和建设顺序。

## Java 必学技术全景（对照本工程）

按「Java 后端开发核心能力」梳理的必学技术，并对照本工程是否已提供 demo：**✅ 已有** / **🟡 部分覆盖** / **❌ 待补**；每项标注重要程度 **🔴 高**（核心必备）/ **🟡 中**（进阶必备）/ **⚪ 低**（专项按需）。

### 推荐学习路线（按重要程度排序）

> 同重要度内按「先打地基 → 再高频中间件 → 后专项」的逻辑排列；✅ 表示本工程已有 demo 可直接对照学，❌/🟡 表示待补，建议学完前面的再补对应 demo。

| # | 重要度 | 技术 | 状态 | 对应模块 / 建议 |
|---|--------|------|------|----------------|
| 1 | 🔴 高 | Java 集合 / 泛型 / 异常 / 反射 / 注解 | ✅ 完成 | `jdk8-base`：反射/异常/SPI 基础 + 源码级分析（HashMap/ConcurrentHashMap 内部、ArrayList 扩容、泛型擦除实战、自定义注解 + APT 处理器），见 `java.base/collection`、`java.base/generic`、`java.base/annotation` 子包，均含 `*Test` |
| 2 | 🔴 高 | 并发编程 JUC（线程池 / 锁 / AQS / 并发容器） | ✅ | `jdk8-base` |
| 3 | 🔴 高 | JVM（内存模型 / 类加载 / GC） | ✅ 完成 | `jdk8-base`：类加载/CGLib/HeapOOM + GarbageCollectorDemo（Serial/Parallel/CMS/G1/ZGC 对比 + GC 日志），见 `jvm.gc` 子包，含 `GarbageCollectorDemoTest`；调优参数/arthas 见进阶说明 |
| 4 | 🔴 高 | I/O 与网络（BIO / NIO / Netty / 零拷贝 / Reactor） | ✅ | `jdk8-base` |
| 5 | 🔴 高 | Spring / Spring Boot（IOC / AOP / 自动装配） | ✅ | 全工程基础 |
| 6 | 🔴 高 | Servlet / Filter / Listener（手写容器） | ✅ | `jdk8-base` 迷你 Servlet 容器 |
| 7 | 🔴 高 | MyBatis / MyBatis-Plus（ORM 持久层） | ✅ | `jdk8-mybatis-plus-demo`（条件构造器/分页/逻辑删除+乐观锁/多租户/动态表名/字段加密） |
| 8 | 🔴 高 | Redis（缓存 / 分布式锁 / Lua） | ✅ | `jdk8-redis-demo` |
| 9 | 🔴 高 | 单元测试（JUnit5 / Mockito） | ✅ 完成 | `jdk8-testing-demo`：Mockito `@Mock/@Spy/@InjectMocks`、参数匹配与行为验证（`argThat`/`verify`）、BDD 风格（given/when/then）、Spring Boot 切片测试（`@WebMvcTest`），各场景均含可断言 `*Test` |
| 10 | 🔴 高 | 注册 / 配置中心（Nacos） | ✅ | `jdk8-nacos-demo` |
| 11 | 🔴 高 | 熔断限流（Sentinel） | ✅ | `jdk8-sentinel-demo` |
| 12 | 🔴 高 | 分布式锁（Redis / ZooKeeper） | 🟡 部分 | `jdk8-redis-demo`（Redisson 可重入锁）/ `jdk8-seckill-demo`（Redisson 锁）/ `jdk8-zookeeper-demo`（Curator InterProcessMutex 临时顺序节点锁，Redis vs ZK 锁对比见其 WHY 注释）。ZooKeeper 锁部分见 #25 |
| 13 | 🔴 高 | 分布式 ID（Snowflake / Leaf / TinyId） | ✅ | `jdk8-base` |
| 14 | 🔴 高 | 构建工具（Maven） | ✅ | 本工程 |
| 15 | 🟡 中 | Java 8+ 新特性（Lambda / Stream / Optional） | ✅ | `jdk8-base` |
| 16 | 🟡 中 | 字节码与 Java Agent（ASM / ByteBuddy） | 🟡 部分 | `jdk8-base`（Premain/Agent-Class）。缺：ASM ClassVisitor/ClassWriter 字节码操作、ByteBuddy 运行时动态代理与方法拦截。建议在 `jdk8-base` 中补充 `java.agent.asm` 子包 |
| 17 | 🟡 中 | Spring MVC / WebFlux（响应式） | 🟡 / ❌ | MVC 间接覆盖；WebFlux 待补：RouterFunction 函数式路由、WebClient 异步非阻塞调用、Reactor（Mono/Flux）背压机制。JDK8 下 WebFlux 需 Spring Boot 2.x 支持 |
| 18 | 🟡 中 | 数据库连接池（Druid / HikariCP） | ✅ | 依赖已引入 |
| 19 | 🟡 中 | 本地缓存（Caffeine / Guava Cache） | ✅ | `jdk8-localcache-demo`（★ 后续 A 类 demo 模板） |
| 20 | 🟡 中 | 消息队列 RocketMQ | ✅ | `jdk8-rocketmq-demo` |
| 21 | 🟡 中 | 消息队列 Kafka | ✅ | `jdk8-kafka-demo` |
| 22 | 🟡 中 | 分布式事务（Seata AT/TCC） | ✅ | `jdk8-seata-demo` |
| 23 | 🟡 中 | 网关（Spring Cloud Gateway） | ❌ 待补 | 建议建 `jdk8-gateway-demo`：路由断言工厂（Path/Host/Header）、GatewayFilter 过滤器链、集成 Sentinel 限流 + Nacos 动态路由。**依赖 #10 Nacos + #11 Sentinel，学完后补** |
| 24 | 🟡 中 | 链路追踪（SkyWalking / Sleuth+Zipkin） | ❌ 待补 | 建议建 `jdk8-tracing-demo`：SkyWalking Agent 自动探针 + RocketMQ/Kafka 跨进程追踪、Sleuth（TraceId/SpanId）+ Brave。**依赖 #10 Nacos + #20/#21 消息队列，学完后补** |
| 25 | 🟡 中 | ZooKeeper（协调 / 选主 / 配置） | ✅ 完成 | `jdk8-zookeeper-demo`：Curator 客户端、分布式锁（InterProcessMutex 临时顺序节点）、Leader 选举（LeaderSelector）、配置中心（NodeCache）+ Watcher 监听，含 docker-compose，无 ZK 时测试优雅跳过 |
| 26 | 🟡 中 | 一致性（Paxos / Raft / ZAB） | ✅ 完成 | `jdk8-base` 最小 Basic Paxos + 新增 Raft 选举/日志复制确定性模拟（内存状态机、可断言测试）。ZAB 与 Raft 对比见 Raft 包 WHY 注释 |
| 27 | 🟡 中 | 安全框架（Spring Security / Shiro） | ✅ 完成 | `jdk8-security-demo`：Spring Security 过滤器链（白名单/Basic/无状态）+ JWT 签发校验过滤器（jjwt）+ 方法安全说明；合并 #28 |
| 28 | 🟡 中 | 认证授权（OAuth2 / JWT / Session） | ✅ 完成 | 合并入 `jdk8-security-demo`：JWT 无状态认证 + OAuth2 资源服务器（条件化启用，附 Keycloak docker-compose）+ Session-Cookie 与 Token 方案对比 |
| 29 | 🟡 中 | 加密与签名（AES / RSA / 国密） | ✅ 完成 | `jdk8-crypto-demo`：AES(CBC/GCM)/RSA(加密+SHA256withRSA 签名)/SHA-256/国密 SM2-SM3-SM4（BouncyCastle），含篡改检测断言 |
| 30 | 🟡 中 | 序列化（JSON / Protobuf / Kryo） | ✅ 完成 | `jdk8-serialization-demo`：Jackson 文本 / Kryo 二进制 / JDK 原生三方案同模型对比（往返正确性、体积对比、健壮性、反序列化安全坑）；Protobuf 见进阶方向（未独立建） |
| 31 | 🟡 中 | 监控（Actuator / Prometheus / Grafana） | ❌ 待补 | 建议建 `jdk8-monitor-demo`：Spring Boot Actuator 端点（health/metrics/env）、Micrometer 指标暴露、Prometheus 采集 + Grafana 仪表盘、自定义业务指标（Counter/Gauge/Timer） |
| 32 | 🟡 中 | 定时任务（XXL-JOB / Quartz / Scheduling） | ✅ 完成 | `jdk8-scheduler-demo`：@Scheduled（三种触发模型）/ Quartz（内存 RAMJobStore）/ XXL-JOB（执行端+分片处理器，admin 条件化启用） |
| 33 | 🟡 中 | 对象映射（MapStruct） | ✅ | `jdk8-mapstruct-demo` |
| 34 | 🟡 中 | 搜索引擎（Elasticsearch） | ✅ | `jdk8-elasticsearch-demo` |
| 35 | 🟡 中 | 容器与编排（Docker / Kubernetes） | 🟡 部分 | 部分 demo 提供 docker-compose。缺：Dockerfile 多阶段构建最佳实践（Spring Boot 分层 JAR）、K8s Deployment/Service/ConfigMap 部署模板、本地 k3s/minikube 一键部署脚本。建议在各 demo 中逐步补充 K8s 部署清单 |
| 36 | 🟡 中 | CI/CD（GitLab CI / GitHub Actions） | ❌ 待补 | 建议工程级补充：`.github/workflows/build.yml`（Maven 编译 + 测试 + 打包）、`.gitlab-ci.yml`（多阶段流水线）。非 demo 级，而是本工程根目录补 CI 配置 |
| 37 | ⚪ 低 | JPA / Hibernate（ORM 另一选型） | ❌ 待补 | 与 MyBatis 二选一。建议建 `jdk8-jpa-demo`：Spring Data JPA 仓库方法命名查询、@Query JPQL、实体关联（@OneToMany/@ManyToMany）、二级缓存 |
| 38 | ⚪ 低 | Lucene（搜索底层） | ❌ 进阶 | 进阶了解：Lucene 索引构建（倒排索引）、分词器（IK Analyzer）、查询解析、高亮。建议在 `jdk8-elasticsearch-demo` 中补充 Lucene 底层示例 |
| 39 | ⚪ 低 | 多级缓存（本地 + Redis） | ❌ 待补 | 进阶实战：Caffeine + Redis 两级缓存架构、缓存同步策略（主动更新/过期淘汰/消息通知）、热点 key 探测。建议建 `jdk8-multilevel-cache-demo`，**依赖 #8 Redis + #19 Caffeine** |
| 40 | ⚪ 低 | RabbitMQ（消息队列另一选型） | ❌ 待补 | 与 RocketMQ/Kafka 二选一。建议建 `jdk8-rabbitmq-demo`：Exchange 类型（direct/topic/fanout/headers）、消息确认（publisher confirm / consumer ack）、死信队列 + 延迟消息（TTL+DLX）、Spring AMQP |
| 41 | ⚪ 低 | 分库分表（ShardingSphere） | ❌ 待补 | 海量数据专项。建议建 `jdk8-shardingsphere-demo`：ShardingSphere-JDBC 水平分库分表（inline/standard 策略）、读写分离、分布式主键（雪花算法）、绑定表 + 广播表 |

### 分类明细（含重要度）

#### 一、Java 语言与内核（根基）
- 🔴 高 集合 / 泛型 / 异常 / 反射 / 注解 — ✅ 完成（`jdk8-base` 含反射/异常/SPI + 集合源码分析、泛型擦除、自定义注解子包，均含 `*Test`）
- 🔴 高 并发编程 JUC（线程池 / 锁 / AQS / 并发容器 / Fork-Join / Phaser） — ✅ `jdk8-base`
- 🔴 高 JVM（内存模型 / 类加载 / GC / 调优工具 arthas·jstack·jmap） — ✅ 完成（`jdk8-base` 含类加载/CGLib/HeapOOM + GarbageCollectorDemo 各收集器对比，见 `jvm.gc` 子包，含 `GarbageCollectorDemoTest`）
- 🔴 高 I/O 与网络（BIO / NIO / AIO / Netty / 零拷贝 / Reactor） — ✅ `jdk8-base`
- 🟡 中 Java 8+ 新特性（Lambda / Stream / Optional / 日期 API） — ✅ `jdk8-base`
- 🟡 中 字节码与 Java Agent（instrument / ASM / ByteBuddy） — 🟡 部分（`jdk8-base` 含 Premain/Agent-Class；缺 ASM/ByteBuddy，计划在 `jdk8-base` 中补充 `java.agent.asm` 子包）

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
- ⚪ 低 多级缓存（本地 + Redis） — ❌ 待补（计划建 `jdk8-multilevel-cache-demo`）

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
- 🔴 高 分布式锁（Redis / ZooKeeper） — 🟡 Redis 部分（`jdk8-redis-demo` / `jdk8-seckill-demo`，ZooKeeper 锁并入 #25 ZK demo）
- 🟡 中 ZooKeeper（协调 / 选主 / 配置） — ✅ 完成（`jdk8-zookeeper-demo`）
- 🟡 中 一致性（Paxos / Raft / ZAB） — ✅ 完成（`jdk8-base` 最小 Basic Paxos + Raft 选举/复制模拟）
- ⚪ 低 分库分表（ShardingSphere） — ❌ 待补

#### 八、安全
- 🟡 中 Spring Security / Shiro — ✅ 完成（`jdk8-security-demo`，合并 #27+#28 安全框架 + JWT + OAuth2）
- 🟡 中 认证授权（OAuth2 / JWT / Session） — ✅ 完成（`jdk8-security-demo`）
- 🟡 中 加密与签名（AES / RSA / 国密） — ✅ 完成（`jdk8-crypto-demo`）

#### 九、工具、测试与序列化
- 🔴 高 单元测试（JUnit5 / Mockito） — ✅ 完成（`jdk8-testing-demo`：Mockito `@Mock/@Spy/@InjectMocks`、参数匹配与行为验证、BDD 风格、@WebMvcTest 切片测试）
- 🟡 中 对象映射（MapStruct） — ✅ `jdk8-mapstruct-demo`
- 🟡 中 序列化（JSON / Protobuf / Kryo） — ✅ 完成（`jdk8-serialization-demo`）
- 🟡 中 定时任务（XXL-JOB / Quartz / Scheduling） — ✅ 完成（`jdk8-scheduler-demo`）

#### 十、工程化与运维
- 🔴 高 构建（Maven / Gradle） — ✅ 本工程
- 🟡 中 容器与编排（Docker / Kubernetes） — 🟡 部分 demo 提供 docker-compose（计划逐步为各 demo 补充 K8s 部署清单）
- 🟡 中 监控（Actuator / Prometheus / Grafana） — ❌ 待补（计划建 `jdk8-monitor-demo`）
- 🟡 中 CI/CD（GitLab CI / GitHub Actions） — ❌ 待补（工程级补充）

### 补充计划与优先级（按投入产出比排序）

> 下面列出了所有待补 demo 的建设优先级。优先级公式：**重要程度 × 补充难度 × 依赖链**。分「立即补」「按序补」「可选补」三档。

#### 🔴 第一档：立即补充（高重要度，补充难度低，可复用现有 demo）

| 优先级 | 对应 # | 补什么 | 补到哪里 | 说明 |
|--------|--------|--------|----------|------|
| P0 | #1 | 集合（HashMap 源码级分析）、泛型、注解 + APT | `jdk8-base` 内增 `java.base/collection`、`generic`、`annotation` 子包 | JDK 根基中的根基，当前仅覆盖反射/异常/SPI |
| P0 | #3 | JVM GC 算法对比、调优参数实战、arthas 诊断 | `jdk8-base` 内增 `jvm.gc` 子包 + 调优指南 | 面试高频 + 线上排障必备 |
| P0 | #9 | Mockito 专项 + Spring Boot 切片测试 | ✅ 已完成 `jdk8-testing-demo` | 🔴 高重要度已补齐，独立 demo |
| P0 | #26 | Raft 选举 + 日志复制模拟 | `jdk8-base` 内补充 Raft 共识模拟 | 一致性算法理解分布式锁、选主、配置中心的基础 |

#### 🟡 第二档：按序补充（中重要度，有依赖链，需独立建 demo）

| 优先级 | 对应 # | 补什么 | 模块名 | 前置依赖 |
|--------|--------|--------|--------|----------|
| P1 | #25 | ZooKeeper（分布式锁/选主/配置/Watcher） | `jdk8-zookeeper-demo` ✅ 已完成 | — |
| P1 | #27+#28 | Spring Security + JWT + OAuth2（合并） | `jdk8-security-demo` | — |
| P1 | #29 | 加密与签名（AES/RSA/SHA/国密） | `jdk8-crypto-demo` | — |
| P1 | #30 | 序列化（Jackson/Protobuf/Kryo） | `jdk8-serialization-demo` | — |
| P1 | #32 | 定时任务（@Scheduled/Quartz/XXL-JOB） | `jdk8-scheduler-demo` | — |
| P2 | #23 | Spring Cloud Gateway 网关 | `jdk8-gateway-demo` | #10 Nacos + #11 Sentinel |
| P2 | #24 | SkyWalking 链路追踪 | `jdk8-tracing-demo` | #10 Nacos + #20/#21 MQ |
| P2 | #31 | Actuator + Prometheus + Grafana 监控 | `jdk8-monitor-demo` | #35 Docker |
| P2 | #17 | WebFlux 响应式编程 | `jdk8-webflux-demo` | — |

#### ⚪ 第三档：可选补充（低重要度或非 JDK8 平台核心）

| 优先级 | 对应 # | 补什么 | 模块名 |
|--------|--------|--------|--------|
| P3 | #35 | K8s 部署清单（Dockerfile/Jenkinsfile/K8s YAML） | 工程级补充 |
| P3 | #36 | GitHub Actions + GitLab CI | 工程级补充 |
| P3 | #37 | JPA/Hibernate | `jdk8-jpa-demo` |
| P3 | #39 | 多级缓存（Caffeine + Redis） | `jdk8-multilevel-cache-demo` |
| P3 | #40 | RabbitMQ | `jdk8-rabbitmq-demo` |
| P3 | #41 | ShardingSphere 分库分表 | `jdk8-shardingsphere-demo` |
| P4 | #38 | Lucene 底层 | `jdk8-elasticsearch-demo` 内补充 |

> **新建 demo 的模板**：所有新增 demo 务必参照 `jdk8-localcache-demo`（★ 标杆），严格遵循根目录 `AGENTS.md` 的「AI 生成自检清单」（根包 `lan.chaos.<tech>`、WHY 注释、可观察输出、可断言测试、README 七段式）。



