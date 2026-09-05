# chaos-java

> Java 多版本生态下「**中间件最小可用设计**」学习聚合仓库。以 `jdk<version>-platform` 划分 Maven 聚合工程，一个平台承载一批可独立运行的 Demo，一个模块 = 一个技术点。

## 从这里开始

| 我想… | 去这里 |
|---|---|
| 找某个技术点的可运行实现 | **[技术点索引 `TECH-INDEX.md`](TECH-INDEX.md)** —— 按技术域组织，覆盖全部 5 个 JDK 平台 |
| 查还缺什么、按什么顺序补 | [`TECH-INDEX.md` 末尾的 roadmap](TECH-INDEX.md#待补技术点与建设优先级) —— 11 项未完成 + P2–P4 优先级与依赖链 |
| 了解平台划分与 Demo 规范 | [`AGENTS.md`](AGENTS.md) |
| 浏览模块树 | 继续往下 |

## 本仓库的三个差异点

1. **五 JDK 版本矩阵**：同一技术点在不同 JDK 下的实现差异，都是真实约束踩出来的——MyBatis-Plus 拦截器因 `mybatis-plus-jsqlparser` 需 JDK 11+ 字节码，从 `jdk8` 整体迁至 `jdk11`；Flink 1.17 不兼容 JDK 21，Flink CDC 只能留在 `jdk8-tech`。
2. **优化类 Demo 带量化结论**：批量入库吞吐 3.2×（Redis 命令量降约 420 倍）、虚拟线程 vs 平台线程 IO 密集压测与落地边界、热路径 Filter 异步化的吞吐/P99/忙线程对照、多租户限流三实现的吞吐与 Redis 负载对比。
3. **笔记 ↔ 代码双向可跳**：[`chaos-notes`](https://github.com/xieqiansong/chaos-notes) 中的原理与压测数据均对应到本仓库可运行模块，见 [`TECH-INDEX.md`](TECH-INDEX.md) 的「笔记」列。

> 本文件只做模块树形概述；平台划分、模块命名等全部约束见 [`AGENTS.md`](AGENTS.md)。

## 模块总览

### jdk8-platform · JDK 8 · Spring Boot 2.7.18

默认位：承载对 JDK 无强约束的技术，共 28 个 Maven 模块。

```
jdk8-platform/
├── jdk8-batch-ingest-demo/          批量入库引擎：内存攒批 + 水位触发 + 批量大小在线寻优
├── jdk8-base/                       Java 基础内功 + JDK8 新特性合集（分布式 ID / IO-NIO-Netty / JUC / JVM / Agent 等）
├── jdk8-common/                     平台公共基础模块（占位）
├── jdk8-crypto-demo/                加密与签名：AES / RSA / SHA / 国密 SM2-SM3-SM4
├── jdk8-elasticsearch-demo/         Elasticsearch：索引 / 文档 / 搜索 / 聚合
├── jdk8-kafka-demo/                 Kafka：收发 / 批量 / 分区有序 / Exactly-Once 事务 / 重试死信
├── jdk8-localcache-demo/            Caffeine 本地缓存：读写 / 过期 / 淘汰 / @Cacheable（★ 标杆模板）
├── jdk8-mapstruct-demo/             MapStruct 对象映射：basic / collection / custom / nested
├── jdk8-microservice-demo/          企业级 Spring Cloud Alibaba 微服务：common 支撑 6 模块 + gateway/auth/user/order
├── jdk8-mybatis-plus-demo/          MyBatis-Plus 高阶用法：Wrapper / 分页 / 多租户 / 动态表名 / 字段加密（内置拦截器版）
├── jdk8-nacos-demo/                 Nacos 注册发现 + 配置中心（provider / consumer / config 多进程）
├── jdk8-office-tech/                办公文档处理组：excel / word / pdf（POI / EasyExcel / PDFBox，含横评）
├── jdk8-ratelimiter-demo/           多租户分布式限流三实现对比（Redis+Lua / 本地+Redis / 纯本地）
├── jdk8-redis-demo/                 Redis 全场景：缓存 / 集合 / 排行榜 / 计数 / 分布式锁 / Lua / Pipeline / PubSub
├── jdk8-rocketmq-demo/              RocketMQ 全场景：消息模型与生产消费进阶模式
├── jdk8-rabbitmq-demo/              RabbitMQ（Spring AMQP）：Exchange 路由 / publisher confirm / 手动 Ack / TTL+DLX 死信与延迟
├── jdk8-mqtt-demo/                  MQTT（Eclipse Paho）：发布订阅 / 通配符订阅 / QoS 三等级 / 保留消息 / 遗嘱消息（LWT）
├── jdk8-scheduler-demo/             定时任务：@Scheduled / Quartz / XXL-JOB
├── jdk8-seata-demo/                 分布式事务：AT / TCC / SAGA / XA
├── jdk8-seckill-demo/               秒杀综合实战：分桶库存 + Lua 扣减 + Kafka 异步下单
├── jdk8-security-demo/              Spring Security 过滤器链 + JWT + OAuth2 资源服务器
├── jdk8-sentinel-demo/              流控 / 熔断 / 热点参数 / @SentinelResource
├── jdk8-serialization-demo/         序列化对比：Jackson / Kryo / JDK 原生
├── jdk8-servlet-filter-async-demo/  热路径 Servlet Filter 异步化（绕过 DispatcherServlet）
├── jdk8-short-link-demo/            短链综合实战：Snowflake+Base62 + 布隆过滤 + Redis 缓存
├── jdk8-webflux-demo/              WebFlux 响应式编程：Reactor / 背压 / RouterFunction / WebClient
├── jdk8-starter-demo/               Spring Boot Starter 自动装配机制
├── jdk8-tech/                       JDK8 专属技术点组：flink-cdc-sync / hmac-auth / bitmap-stat
└── jdk8-testing-demo/               JUnit5 + Mockito 测试专项（Mock / BDD / 切片测试）
```

### jdk11-platform · JDK 11 · Spring Boot 2.7.18

承载需 JDK 11+ 的技术。

```
jdk11-platform/
├── jdk11-base/              JDK11 新特性（含 JDK9/10 引入）：String / Files / Optional / Stream / HttpClient / var
├── jdk11-common/            平台公共基础模块（占位）
└── jdk11-mybatis-plus-demo/ MyBatis-Plus 高阶用法（独立拦截器链 mybatis-plus-jsqlparser 版，需 JDK 11+）
```

### jdk17-platform · JDK 17 · Spring Boot 3.5.14

承载需 JDK 17+ 的技术。

```
jdk17-platform/
├── jdk17-base/              JDK17 新特性：文本块 / Record / 密封类 / Switch 表达式 / instanceof 模式匹配
├── jdk17-common/            平台公共基础模块（占位）
├── jdk17-springai-demo/     Spring AI：chat / stream / memory / prompt / 结构化输出 / 工具调用 / RAG / MCP 客户端
└── jdk17-mcp-server-demo/   最小 MCP 服务端（SSE 传输，供 springai-demo 的 MCP 客户端连接演示）
```

### jdk21-platform · JDK 21 · Spring Boot 3.5.14

承载需 JDK 21+ 的技术。

```
jdk21-platform/
├── jdk21-base/              JDK21 新特性：虚拟线程 / Sequenced 集合 / 模式匹配 switch / Record 模式
├── jdk21-common/            平台公共基础模块（占位）
└── jdk21-tech/              JDK21 技术点组：multilevel-cache / idempotent / virtualthread
    ├── jdk21-multilevel-cache-demo/ 多级缓存：Caffeine L1 + Redis Hash L2 + 版本号一致性
    ├── jdk21-idempotent-demo/       接口幂等：请求级 / 消费级 / 状态机三层去重
    └── jdk21-virtualthread-demo/    虚拟线程：机制演示 + 压测量化
```

### jdk25-platform · JDK 25 · Spring Boot 4.0.7

承载需 JDK 25+ 的技术。

```
jdk25-platform/
├── jdk25-base/              JDK25 新特性：模块导入 / 灵活构造器体 / 隐式类 main / Stream Gatherers / 原始类型模式
└── jdk25-common/            平台公共基础模块（占位）
```

---

各平台模块清单、技术栈与学习记录以模块内 `README.md` 为准；按技术域检索全部平台、以及待补技术点与建设优先级，均见 [`TECH-INDEX.md`](TECH-INDEX.md)。
