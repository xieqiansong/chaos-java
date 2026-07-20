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

## 已完成学习记录

| 模块 | 状态 | 说明 |
|------|------|------|
| `jdk8-common` | 🟡 占位 | 公共基础模块占位（`App.java`），后续承载跨 demo 的公共工具/实体 |
| `jdk8-mapstruct-demo` | ✅ 完成 | MapStruct 对象映射：basic / collection / custom / nested，配套单元测试 |
| `jdk8-mybatis-plus-demo` | ⚪ 未开始 | 已建模块骨架，待补充 MyBatis-Plus 示例 |
| `jdk8-nacos-demo` | ✅ 完成 | Nacos 服务注册发现 + 配置中心：nacos-common / nacos-provider / nacos-consumer / nacos-config |
| `jdk8-rocketmq-demo` | ✅ 完成 | RocketMQ 全场景：simple / batch / broadcast / delay / filter / order / pull / requestreply / retry / transaction / trace / throttle / acl / faulttolerant 等 |
| `jdk8-redis-demo` | ✅ 完成 | Redis 全场景：字符串/对象缓存、Hash/List/Set、ZSet 排行榜、计数、分布式锁、Lua 限流/扣库存、Pipeline、PubSub（单元测试驱动） |
| `jdk8-kafka-demo` | ✅ 完成 | Kafka 全场景：基础收发、批量发送/消费、分区有序、Exactly-Once 事务、重试/死信、Header 消息过滤（@EmbeddedKafka 自包含测试） |
| `jdk8-seata-demo` | ✅ 完成 | Seata 分布式事务：AT 自动补偿 + TCC Try/Confirm/Cancel，H2 内存数据库自包含测试 |
| `jdk8-sentinel-demo` | ✅ 完成 | Sentinel 流控/熔断/热点参数/@SentinelResource，SphU.entry() 程序化 + 注解双轨，自包含测试 |

### 模块详情

- **jdk8-mapstruct-demo**：覆盖基础字段映射、集合映射、自定义方法（`@Mapping` + `MappingUtil`）、嵌套对象映射；`target/generated-sources` 下已生成 `XxxMapperImpl` 验证注解处理器生效，测试覆盖 basic / collection / custom / nested。
- **jdk8-nacos-demo**：基于 Spring Cloud Alibaba，演示 Provider 注册、`@LoadBalanced` RestTemplate / OpenFeign 消费、配置中心 `@RefreshScope` 动态刷新与编程式 `ConfigService.addListener` 监听。
- **jdk8-rocketmq-demo**：使用原生 RocketMQ Client，以独立类演示各类消息模型与生产消费模式，并包含幂等、重试、死信、事务、ACL、故障容错、消息轨迹等进阶能力。
- **jdk8-redis-demo**：基于 Spring Data Redis + Lettuce，按能力分包（cache/collection/rank/counter/lock/ratelimit/stock/pipeline/pubsub），以单元测试为核心验证手段。
- **jdk8-kafka-demo**：基于 Spring Kafka 2.8.x，覆盖基础收发、批量发送/消费、分区有序、Exactly-Once 事务、重试/死信、Header 消息过滤，@EmbeddedKafka 自包含测试无需外部 Broker。
- **jdk8-seata-demo**：基于 Seata 1.6.x + Spring Cloud Alibaba，覆盖 AT 模式（自动 undo_log 补偿）和 TCC 模式（手动 Try/Confirm/Cancel），JdbcTemplate 直操作业聚焦事务概念，H2 内存数据库自包含测试。
- **jdk8-sentinel-demo**：基于 Sentinel 1.8.6 + Spring Cloud Alibaba，覆盖 QPS 直接/关联/WarmUp 流控、异常数/异常比例/慢调用比例熔断、热点参数限流、@SentinelResource 注解（blockHandler/fallback），SphU.entry() 程序化方式核心稳定，包含 Dashboard docker-compose。

## 迁移计划（来自 chaos-java-example-old）

> 目标：把 `chaos-java-example-old` 中的示例逐步迁移到本工程，统一命名（`jdk8-*-demo`）、统一版本管理，并补全 README / 测试。以下仅做任务记录，暂不写代码。

### 已迁移（可直接对照 old 目录复核）

| 原模块 | 目标模块 | 备注 |
|--------|----------|------|
| `chaos-java-example-old/nacos-demo` | `jdk8-nacos-demo` | 已迁移并完善（多环境/共享配置 old 中有，可酌情补充） |
| `chaos-java-example-old/rocketmq-demo` | `jdk8-rocketmq-demo` | 已迁移并扩展更多场景 |
| `chaos-java-example-old/redis-demo` | `jdk8-redis-demo` | 已迁移：HTTP 场景入口 + 新增 `/redis` 场景清单 + 修复空 RedisConfig 与 getUser 路径变量 |

### 待迁移任务清单

| # | 原模块 | 目标模块 | 优先级 | 说明 |
|---|--------|----------|--------|------|
| 1 | `redis-demo` | `jdk8-redis-demo` | 高 | ✅ 已迁移（见上方「已完成学习记录」「已迁移」表） |
| 2 | `kafka-demo` | `jdk8-kafka-demo` | 中 | ✅ 已迁移 |
| 3 | `seata-demo` | `jdk8-seata-demo` | 中 | ✅ 已迁移：AT + TCC 两种模式，H2 自包含测试 |
| 4 | `sentinel-demo` | `jdk8-sentinel-demo` | 中 | ✅ 已迁移：流控/熔断/热点/@SentinelResource，SphU 程序化 + 注解双轨 |
| 5 | `elasticsearch-demo` | `jdk8-elasticsearch-demo` | 中 | ES 索引/文档/搜索（old 中为注释模块） |
| 6 | `demo-seckill` | `jdk8-seckill-demo` | 高 | 秒杀系统：超卖/库存扣减（含 lua、sql、设计文档 `开发计划.md` 等） |
| 7 | `demo-short-link` | `jdk8-short-link-demo` | 高 | 短链生成与跳转服务（含 sql、设计文档） |
| 8 | `java-guide-demos` | `jdk8-java-guide-demos` | 低 | Java 基础/Guide 系列示例集合 |
| 9 | `tech-pdai-java-demos` | `jdk8-tech-pdai-java-demos` | 低 | tech-pdai Java 示例集合 |
| 10 | `tech-pdai-spring-demos` | `jdk8-tech-pdai-spring-demos` | 低 | tech-pdai Spring 示例集合（old 中为注释模块，体量大，按需迁移） |
| 11 | `chaos-test` | `jdk8-chaos-test` | 低 | 测试相关示例 |
| 12 | `javaagent` | `jdk8-javaagent-demo` | 低 | Java Agent 字节码增强示例（old 中 src 为空，需补全） |
| 13 | `common` | `jdk8-common` | — | old 中为空，功能并入现有 `jdk8-common` 占位模块 |

### 迁移步骤（通用）

1. 在 `pom.xml` 的 `<modules>` 中注册新子模块 `jdk8-xxx-demo`。
2. 新建子模块 `pom.xml`，继承父工程并复用 `dependencyManagement` 版本（不再写死版本号）。
3. 迁移源码到 `src/main/java/lan/chaos/xxx`，包名统一改为 `lan.chaos.xxx`。
4. 迁移 `src/main/resources` 配置与 `target` 外的资源（sql / lua / 文档）。
5. 补全该子模块的 README（参考 `redis-demo/README.md` 的详实风格）。
6. 对核心逻辑补充单元测试（参考 `jdk8-mapstruct-demo` 的测试写法）。
7. 本地 `mvn -pl jdk8-xxx-demo -am clean install -DskipTests` 验证可编译。

### 暂不迁移

- 若某 old 模块仅为占位或体量大且学习价值低，可在迁移时标记为「跳过」并记录原因。
- `tech-pdai-spring-demos` 体量大（1000+ 文件），建议拆分为多个独立 `jdk8-*` 子任务按需迁移。
