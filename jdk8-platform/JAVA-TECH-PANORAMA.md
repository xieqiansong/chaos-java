# Java 必学技术全景（对照本工程）

> 本文件从 `jdk8-platform/README.md` 拆解而来，是「Java 后端必学技术」对照本工程 demo 覆盖情况的独立索引。各 demo 的状态与说明以 `README.md` 的「模块总览 / 模块详情」为准。

按「Java 后端开发核心能力」梳理的必学技术，并对照本工程是否已提供 demo：**✅ 已有** / **🟡 部分覆盖** / **❌ 待补**；每项标注重要程度 **🔴 高**（核心必备）/ **🟡 中**（进阶必备）/ **⚪ 低**（专项按需）。

### 推荐学习路线（按重要程度排序）

> 同重要度内按「先打地基 → 再高频中间件 → 后专项」的逻辑排列；✅ 表示本工程已有 demo 可直接对照学，❌/🟡 表示待补，建议学完前面的再补对应 demo。

| # | 重要度 | 技术 | 状态 | 对应模块 / 建议 |
|---|--------|------|------|----------------|
| 1 | 🔴 高 | Java 集合 / 泛型 / 异常 / 反射 / 注解 | ✅ 完成 | `jdk8-base`：反射/异常/SPI 基础 + 源码级分析（HashMap/ConcurrentHashMap 内部、ArrayList 扩容、泛型擦除实战、自定义注解运行时处理 + 反射解析，APT 注解处理器暂未实现），见 `java.base/collection`、`java.base/generic`、`java.base/annotation` 子包，均含 `*Test` |
| 2 | 🔴 高 | 并发编程 JUC（线程池 / 锁 / AQS / 并发容器） | ✅ 完成 | `jdk8-base` |
| 3 | 🔴 高 | JVM（内存模型 / 类加载 / GC） | ✅ 完成 | `jdk8-base`：类加载/CGLib/HeapOOM + GarbageCollectorDemo（Serial/Parallel/CMS/G1/ZGC 对比 + GC 日志），见 `jvm.gc` 子包，含 `GarbageCollectorDemoTest`；调优参数/arthas 见进阶说明 |
| 4 | 🔴 高 | I/O 与网络（BIO / NIO / Netty / 零拷贝 / Reactor） | ✅ 完成 | `jdk8-base` |
| 5 | 🔴 高 | Spring / Spring Boot（IOC / AOP / 自动装配） | ✅ 完成 | 全工程基础 |
| 6 | 🔴 高 | Servlet / Filter / Listener（手写容器） | ✅ 完成 | `jdk8-base` 迷你 Servlet 容器 |
| 7 | 🔴 高 | MyBatis / MyBatis-Plus（ORM 持久层） | ✅ 完成 | `jdk8-mybatis-plus-demo`（条件构造器/分页/逻辑删除+乐观锁/多租户/动态表名/字段加密） |
| 8 | 🔴 高 | Redis（缓存 / 分布式锁 / Lua） | ✅ 完成 | `jdk8-redis-demo` |
| 9 | 🔴 高 | 单元测试（JUnit5 / Mockito） | ✅ 完成 | `jdk8-testing-demo`：Mockito `@Mock/@Spy/@InjectMocks`、参数匹配与行为验证（`argThat`/`verify`）、BDD 风格（given/when/then）、Spring Boot 切片测试（`@WebMvcTest`），各场景均含可断言 `*Test` |
| 10 | 🔴 高 | 注册 / 配置中心（Nacos） | ✅ 完成 | `jdk8-nacos-demo` |
| 11 | 🔴 高 | 熔断限流（Sentinel） | ✅ 完成 | `jdk8-sentinel-demo` |
| 12 | 🔴 高 | 分布式锁（Redis / ZooKeeper） | ✅ 完成 | `jdk8-redis-demo`（Redisson 可重入锁）/ `jdk8-seckill-demo`（Redisson 锁）/ `jdk8-zookeeper-demo`（Curator InterProcessMutex 临时顺序节点锁，Redis vs ZK 锁对比见其 WHY 注释） |
| 13 | 🔴 高 | 分布式 ID（Snowflake / Leaf / TinyId） | ✅ 完成 | `jdk8-base` |
| 14 | 🔴 高 | 构建工具（Maven） | ✅ 完成 | 本工程 |
| 15 | 🟡 中 | Java 8+ 新特性（Lambda / Stream / Optional） | ✅ 完成 | `jdk8-base` |
| 16 | 🟡 中 | 字节码与 Java Agent（ASM / ByteBuddy） | 🟡 部分 | `jdk8-base`（Premain/Agent-Class）。缺：ASM ClassVisitor/ClassWriter 字节码操作、ByteBuddy 运行时动态代理与方法拦截。建议在 `jdk8-base` 中补充 `java.agent.asm` 子包 |
| 17 | 🟡 中 | Spring MVC / WebFlux（响应式） | 🟡 / ❌ | MVC 间接覆盖；WebFlux 待补：RouterFunction 函数式路由、WebClient 异步非阻塞调用、Reactor（Mono/Flux）背压机制。JDK8 下 WebFlux 需 Spring Boot 2.x 支持 |
| 18 | 🟡 中 | 数据库连接池（Druid / HikariCP） | ✅ 完成 | 依赖已引入 |
| 19 | 🟡 中 | 本地缓存（Caffeine / Guava Cache） | ✅ 完成 | `jdk8-localcache-demo`（★ 后续 A 类 demo 模板） |
| 20 | 🟡 中 | 消息队列 RocketMQ | ✅ 完成 | `jdk8-rocketmq-demo` |
| 21 | 🟡 中 | 消息队列 Kafka | ✅ 完成 | `jdk8-kafka-demo` |
| 22 | 🟡 中 | 分布式事务（Seata AT/TCC） | ✅ 完成 | `jdk8-seata-demo` |
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
| 33 | 🟡 中 | 对象映射（MapStruct） | ✅ 完成 | `jdk8-mapstruct-demo` |
| 34 | 🟡 中 | 搜索引擎（Elasticsearch） | ✅ 完成 | `jdk8-elasticsearch-demo` |
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
- 🔴 高 分布式锁（Redis / ZooKeeper） — ✅ 完成（`jdk8-redis-demo` / `jdk8-seckill-demo` Redisson 锁 + `jdk8-zookeeper-demo` Curator InterProcessMutex 锁，Redis vs ZK 锁对比见其 WHY 注释）
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
| P0 | #1 | 集合（HashMap 源码级分析）、泛型、注解 + APT | ✅ 已完成（`jdk8-base` 内已增 `java.base/collection`、`generic`、`annotation` 子包，均含 `*Test`） | JDK 根基中的根基，已覆盖反射/异常/SPI + 集合源码分析 + 泛型擦除 + 自定义注解 |
| P0 | #3 | JVM GC 算法对比、调优参数实战、arthas 诊断 | ✅ 已完成（`jdk8-base` 内已增 `jvm.gc` 子包 + `GarbageCollectorDemo`，含 `GarbageCollectorDemoTest`） | 面试高频 + 线上排障必备 |
| P0 | #9 | Mockito 专项 + Spring Boot 切片测试 | ✅ 已完成 `jdk8-testing-demo` | 🔴 高重要度已补齐，独立 demo |
| P0 | #26 | Raft 选举 + 日志复制模拟 | ✅ 已完成（`jdk8-base` 内已增 Raft 选举/日志复制确定性模拟，含可断言测试） | 一致性算法理解分布式锁、选主、配置中心的基础 |

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
