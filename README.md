# chaos-java

> Java 生态「**中间件最小可用设计**」学习聚合仓库。按**技术域**划分主题目录，每个模块 = 一个技术点；JDK / Spring Boot 版本作为模块 pom 属性（BOM 收口），不再体现在目录名上。

## 从这里开始

| 我想… | 去这里 |
|---|---|
| 找某个技术点的可运行实现 | **[技术点索引 `TECH-INDEX.md`](TECH-INDEX.md)** —— 按技术域组织，覆盖全部模块 |
| 查还缺什么、按什么顺序补 | [`TECH-INDEX.md` 末尾的 roadmap](TECH-INDEX.md#待补技术点与建设优先级) —— 11 项未完成 + P2–P4 优先级与依赖链 |
| 了解目录划分与 Demo 规范 | [`AGENTS.md`](AGENTS.md) |
| 浏览模块树 | 继续往下 |

## 本仓库的三个差异点

1. **JDK / Spring Boot 版本作为模块属性**：同一技术点在不同 SB 主线的差异都是真实约束踩出来的——MyBatis-Plus 拦截器因 `mybatis-plus-jsqlparser` 需 JDK 11+ 字节码，单独建 `mybatis-plus-jdk11-demo`（SB2 / release 11）；Flink 1.17 不兼容 JDK 21，Flink CDC 留在 SB2（release 8）。版本不进目录名，见 [`AGENTS.md`](AGENTS.md)。
2. **优化类 Demo 带量化结论**：批量入库吞吐 3.2×（Redis 命令量降约 420 倍）、虚拟线程 vs 平台线程 IO 密集压测与落地边界、热路径 Filter 异步化的吞吐/P99/忙线程对照、多租户限流三实现的吞吐与 Redis 负载对比。
3. **笔记 ↔ 代码双向可跳**：[`chaos-notes`](https://github.com/xieqiansong/chaos-notes) 中的原理与压测数据均对应到本仓库可运行模块，见 [`TECH-INDEX.md`](TECH-INDEX.md) 的「笔记」列。

> 本文件只做模块树形概述；目录划分、模块命名等全部约束见 [`AGENTS.md`](AGENTS.md)。

## 模块总览

按**技术域**组织主题目录；JDK / Spring Boot 版本是每模块的 pom 属性（详见 [`AGENTS.md`](AGENTS.md) 第八章），不体现在目录名。聚合模块（microservice-demo / nacos-demo / ms-common）内部仍按部署/角色单元分子模块。

```
chaos-java/
├── java-core/                           Java 内功合集（SB2 / release 8）
│   └── java-core/                       Java 基础内功 + JDK8 新特性合集（分布式 ID / IO-NIO-Netty / JUC / JVM / Agent / 一致性算法等）
├── jdk-features/                       各版本新特性（版本即主题）
│   ├── jdk11-base/                      JDK11 新特性（String / Files / Optional / Stream / HttpClient / var）
│   ├── jdk17-base/                      JDK17 新特性（文本块 / Record / 密封类 / Switch 表达式 / 模式匹配）
│   ├── jdk21-base/                      JDK21 新特性（虚拟线程 / Sequenced 集合 / 模式匹配 switch / Record 模式）
│   └── jdk25-base/                      JDK25 新特性（模块导入 / 灵活构造器体 / 隐式类 main / Stream Gatherers / 原始类型模式）
├── cache/                              缓存
│   ├── localcache-demo/                 Caffeine 本地缓存（★ 标杆模板）
│   ├── redis-demo/                      Redis 全场景：缓存 / 集合 / 排行榜 / 计数 / 分布式锁 / Lua / Pipeline / PubSub
│   └── multilevel-cache-demo/           多级缓存：Caffeine L1 + Redis Hash L2 + 版本号一致性（JDK 21）
├── mq/                                 消息队列
│   ├── kafka-demo/                      Kafka：收发 / 批量 / 分区有序 / Exactly-Once 事务 / 重试死信
│   ├── rocketmq-demo/                   RocketMQ 全场景
│   ├── rabbitmq-demo/                   RabbitMQ（Spring AMQP）：Exchange 路由 / publisher confirm / 手动 Ack / TTL+DLX
│   └── mqtt-demo/                       MQTT（Eclipse Paho）：发布订阅 / 通配符 / QoS / 保留消息 / 遗嘱消息
├── distributed/                        分布式与协调
│   ├── seata-demo/                      分布式事务：AT / TCC / SAGA / XA
│   ├── ratelimiter-demo/                多租户分布式限流三实现对比（Redis+Lua / 本地+Redis / 纯本地）
│   ├── idempotent-demo/                 接口幂等：请求级 / 消费级 / 状态机三层去重（JDK 21）
│   └── zookeeper-demo/                  ZooKeeper 协调：Curator 锁 / 选主 / 配置中心
├── microservice/                       微服务与治理
│   ├── nacos-demo/                      Nacos 注册发现 + 配置中心（provider / consumer / config 多进程）
│   ├── sentinel-demo/                   流控 / 熔断 / 热点参数 / @SentinelResource
│   ├── security-demo/                   Spring Security 过滤器链 + JWT + OAuth2 资源服务器
│   └── microservice-demo/               企业级 Spring Cloud Alibaba 微服务（common 支撑 6 模块 + gateway/auth/user/order）
├── persistence/                        数据持久层
│   ├── mybatis-plus-jdk8-demo/          MyBatis-Plus 高阶用法（内置拦截器版，SB2 / release 8）
│   ├── mybatis-plus-jdk11-demo/         MyBatis-Plus 高阶用法（独立 mybatis-plus-jsqlparser 拦截器链，需 JDK 11+）
│   ├── batch-ingest-demo/               批量入库引擎：内存攒批 + 水位触发 + 批量大小在线寻优
│   ├── elasticsearch-demo/              Elasticsearch：索引 / 文档 / 搜索 / 聚合
│   └── flink-cdc-sync-demo/             Flink CDC 同源库表同步（Flink 1.17 不兼容 JDK 21，留在 SB2）
├── ai/                                 AI 应用
│   ├── springai-demo/                   Spring AI：chat / stream / memory / prompt / 结构化输出 / 工具调用 / RAG / MCP 客户端（JDK 17）
│   └── mcp-server-demo/                 最小 MCP 服务端（SSE 传输，JDK 17）
├── office/                             办公文档处理
│   ├── excel-demo/                      Excel：POI / EasyExcel / Hutool 三体系横评
│   ├── word-demo/                       文档生成与处理
│   └── pdf-demo/                        PDFBox 处理
├── engineering/                        工程化与测试
│   ├── mapstruct-demo/                  对象映射：basic / collection / custom / nested
│   ├── testing-demo/                    JUnit5 + Mockito 测试专项
│   ├── serialization-demo/              序列化对比：Jackson / Kryo / JDK 原生
│   ├── scheduler-demo/                  定时任务：@Scheduled / Quartz / XXL-JOB
│   ├── starter-demo/                    Spring Boot Starter 自动装配机制
│   ├── webflux-demo/                    WebFlux 响应式编程
│   ├── servlet-filter-async-demo/        热路径 Servlet Filter 异步化
│   ├── virtualthread-demo/              虚拟线程：机制演示 + 压测量化（JDK 21）
│   └── bitmap-stat-demo/                位图统计应用
├── crypto/                             安全与加密
│   ├── crypto-demo/                     主流加密算法全景（对称 / 非对称 / 摘要-MAC / KDF / 国密）
│   └── hmac-auth-demo/                  HMAC 签名鉴权
├── showcase/                           综合实战（保留不新增）
│   ├── seckill-demo/                   秒杀：分桶库存 + Lua 扣减 + Kafka 异步下单
│   ├── short-link-demo/                短链：Snowflake+Base62 + 布隆过滤 + Redis 缓存
│   └── game-leaderboard-demo/          游戏排行榜：Redis ZSet 排行榜
└── tools/                              工具
    └── tools/                          杂项工具：算法题 / 模型 / CPU 负载控制 / 空目录清理等
```

---

各模块清单、技术栈与学习记录以模块内 `README.md` 为准；按技术域检索全部模块、以及待补技术点与建设优先级，见 [`TECH-INDEX.md`](TECH-INDEX.md)。
