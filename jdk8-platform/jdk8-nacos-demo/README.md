# Nacos Demo

Nacos 服务治理演示模块，覆盖**服务注册与发现**与**配置中心**两大核心能力（基于 Spring Cloud Alibaba）。所有场景由独立 Spring Boot 子模块承载，启动后访问 HTTP 接口或观察控制台验证。

- 基础包：`lan.chaos.nacos`
- 技术栈：Spring Cloud Alibaba 2021.0.5.0 + Spring Cloud 2021.0.5 + Spring Boot 2.7.18 + Nacos 2.x（gRPC 默认 `8848` / 控制台 `http://REDACTED:8848/nacos`）
- 触发入口：各子模块 `Application` 启动类 + 暴露的 HTTP 接口（部分场景附 `*Test` 验证）

> 所有场景均连真实 Nacos（`REDACTED:8848`），请先启动 Nacos Server 再启动应用。

> 使用频率标注：`★★★ 高频`（几乎每个微服务项目都会用）／`★★☆ 中频`（常见但不一定都有）／`★☆☆ 低频`（特定场景才需要）／`◆ 基础`（公共模块，非独立业务场景）。

## 目录结构（规划）

```
nacos-demo/
├── pom.xml                              # 父 pom：聚合子模块 + SCA 依赖管理
├── nacos-common/                        # ◆ 公共 API（实体 + OpenFeign 客户端接口）
│   └── src/main/java/lan/chaos/nacos/common
│       ├── User.java                    # 实体
│       └── UserClient.java              # @FeignClient 接口（供 consumer 调用）
├── nacos-provider/                      # 服务提供者（注册到 Nacos） ★★★
│   └── src/main/java/lan/chaos/nacos/provider
│       ├── ProviderApplication.java     # @EnableDiscoveryClient 注册实例
│       └── UserController.java          # 暴露 /user/{id} 接口
│   └── src/main/resources/application.yml
├── nacos-consumer/                      # 服务消费者（发现 + 负载均衡调用） ★★★
│   └── src/main/java/lan/chaos/nacos/consumer
│       ├── ConsumerApplication.java     # @EnableDiscoveryClient
│       ├── ConsumerConfig.java          # @LoadBalanced RestTemplate / 启用 Feign
│       └── OrderController.java         # 调用 provider，观察负载均衡
│   └── src/main/resources/application.yml
└── nacos-config/                        # 配置中心 + 动态刷新 ★★★
    └── src/main/java/lan/chaos/nacos/config
└── nacos-server/                        # Nacos 服务端启动与配置（非业务模块，见其 README）
    ├── docker-compose.yml               # 方式二：Docker 一键起（standalone，映射 8848/9848/9849）
    ├── scripts/start.{cmd,sh}           # 方式一：本机二进制启动包装（需先下载解压 nacos-server）
    └── conf/*.example                   # 端口 / 数据源 / 集群 配置示例
        ├── ConfigApplication.java
        ├── DynamicConfig.java           # @RefreshScope 动态配置 Bean
        ├── ConfigController.java        # 暴露 /config 读取动态配置（@Value / @ConfigurationProperties）
        └── ConfigListener.java          # 编程式 ConfigService.addListener 监听变更
    └── src/main/resources
        ├── bootstrap.yml                # 基础配置（data-id / group / 自动刷新）
        ├── bootstrap-dev.yml            # 多环境隔离（namespace=dev） ★★☆
        ├── bootstrap-test.yml           # 多环境隔离（namespace=test） ★★☆
        └── bootstrap-shared.yml         # 共享/扩展配置与优先级 ★★☆
```

## 场景一览（按使用频率排序）

`★★★ 高频`
- [服务注册与发现 discovery](#1-服务注册与发现-discovery) → 启动 `ProviderApplication` + `ConsumerApplication`，访问 `consumer` 接口
- [配置中心 + 动态刷新 config](#2-配置中心--动态刷新-config) → 启动 `ConfigApplication`，改 Nacos 配置后访问 `/config` 看实时生效
- [@RefreshScope 配置优先级](#4-共享配置与优先级) 见配置章节

`★★☆ 中频`
- [多环境隔离 namespace/group](#3-多环境隔离-namespacegroup) → `bootstrap-dev/test.yml` 切换环境
- [共享配置与优先级](#4-共享配置与优先级) → `bootstrap-shared.yml` 演示 `shared-configs` / `extension-configs`
- [配置变更编程式监听](#5-配置变更编程式监听) → `ConfigListener`
- [服务元数据 / 权重 / 临时持久实例](#6-服务元数据权重与实例类型) → 注册时带 metadata

`◆ 基础模块`
- [nacos-common 公共 API](#nacos-common-公共-api)

---

### nacos-common 公共 API `◆`

服务提供方与消费方共享的实体与 Feign 客户端接口，避免重复定义。

| 类 | 说明 |
|----|------|
| `common/User.java` | 传输实体 |
| `common/UserClient.java` | `@FeignClient("nacos-provider")` 接口，声明 `GET /user/{id}` |

---

### 1. 服务注册与发现 discovery `★★★`

微服务骨架的核心：Provider 启动时把自己注册到 Nacos，Consumer 从 Nacos 拉取实例列表并通过负载均衡发起调用（默认 `Ribbon`/`Spring Cloud LoadBalancer`）。

- 注册：`@EnableDiscoveryClient` + `spring.cloud.nacos.discovery.server-addr`
- 发现：注入 `DiscoveryClient` 或直接用 `@LoadBalanced RestTemplate` / `OpenFeign`

| 类 | 说明 |
|----|------|
| `provider/ProviderApplication.java` | 注册实例，暴露 `/user/{id}` |
| `provider/UserController.java` | 返回示例用户，打印本实例 IP/port |
| `consumer/ConsumerApplication.java` | 消费方注册 |
| `consumer/ConsumerConfig.java` | 配置 `@LoadBalanced` RestTemplate 或开启 Feign |
| `consumer/OrderController.java` | 调 `provider` 接口，多实例部署可观察轮询 |

触发：先后启动 Provider / Consumer，访问 `http://localhost:<consumer-port>/order/1`。

---

### 2. 配置中心 + 动态刷新 config `★★★`

把配置从本地 `application.yml` 外移到 Nacos（`data-id` + `group`），应用无需重启即可感知变更——这是 Nacos 相比"本地配置"最大的价值。

- 基础依赖：`spring-cloud-starter-alibaba-nacos-config`
- 动态刷新：在 Bean 上标 `@RefreshScope`，Nacos 配置变更后下次注入即生效
- 注意：Nacos Config 在 `bootstrap` 阶段读取，配置写 `bootstrap.yml` 而非 `application.yml`

| 类 | 说明 |
|----|------|
| `config/ConfigApplication.java` | 启动应用 |
| `config/DynamicConfig.java` | `@RefreshScope` 的 `@ConfigurationProperties` Bean |
| `config/ConfigController.java` | 暴露 `/config`，对比 `@Value` 与 `@ConfigurationProperties` 两种读取方式 |
| `config/ConfigListener.java` | 编程式 `ConfigService.addListener` 监听变更（见场景 5） |

触发：启动 `ConfigApplication` → 访问 `/config` → 在 Nacos 控制台改配置 → 再访问 `/config` 看到新值。

---

### 3. 多环境隔离 namespace/group `★★☆`

用 `namespace`（环境：dev/test/prod）+ `group`（业务分组）逻辑隔离配置与实例，避免互相污染。环境用 `namespace` 隔离最彻底（不同命名空间互不可见）。

| 文件 | 说明 |
|------|------|
| `bootstrap-dev.yml` | `namespace=dev` 的命名空间 ID |
| `bootstrap-test.yml` | `namespace=test` 的命名空间 ID |
| `bootstrap-prod.yml` | `namespace=prod` 的命名空间 ID |

触发：以不同 profile 启动（`--spring.profiles.active=dev`），观察拉到的配置来自不同 namespace。

---

### 4. 共享配置与优先级 `★★☆`

多个应用复用同一份基础配置（如 DB、Redis）时，用 `shared-configs`（共享）与 `extension-configs`（扩展），并理解其覆盖优先级：

`远程 data-id（自身） > extension-configs > shared-configs > 本地 application.yml`

| 文件 | 说明 |
|------|------|
| `bootstrap-shared.yml` | 演示 `spring.cloud.nacos.config.shared-configs` / `extension-configs` 与优先级 |

---

### 5. 配置变更编程式监听 `★★☆`

除 `@RefreshScope` 自动刷新外，也可编程式监听：`ConfigService.addListener(dataId, group, listener)`，在 `receiveConfigInfo` 回调里做自定义处理（如热更新线程池、重新加载规则）。

| 类 | 说明 |
|----|------|
| `config/ConfigListener.java` | `addListener` 注册，回调打印新配置并触发自定义逻辑 |

---

### 6. 服务元数据 / 权重与实例类型 `★★☆`

- **元数据（metadata）**：注册时携带自定义键值，供灰度路由、版本隔离等使用。
- **权重（weight）**：控制台调整实例权重，影响负载均衡分配比例。
- **临时 vs 持久实例**：默认临时实例（心跳保活，掉线自动剔除）；持久实例（注册信息落盘，适合需"永久在线"的基础服务）。

触发：启动时通过 `spring.cloud.nacos.discovery.metadata.*` 与 `ephemeral=false` 配置，在 Nacos 控制台「服务详情」查看。

---

## 如何运行

```bash
# 启动 Nacos Server（默认 8848）
# 父工程编译
mvn -pl nacos-common -am install -DskipTests
mvn -pl nacos-provider,nacos-consumer -am spring-boot:run
mvn -pl nacos-config -am spring-boot:run
```

## 进阶方向（依赖外部组件 / 独立部署，未写成独立 Demo）

- `◆` **Nacos 集群部署**：3 节点 + 外置 MySQL，生产高可用（需部署 Nacos 集群与 DB）
- `◆` **配置加密**：敏感配置（密码）结合 KMS / AES 加密（需密钥基础设施）
- `◆` **Nacos + Sentinel 联动**：把 Sentinel 限流/熔断规则存放到 Nacos 做动态推送（见 sentinel-demo 动态规则源）

## 设计要点

- **聚合工程**：`nacos-demo` 为父 pom，按"注册发现 / 配置中心"拆成多个可独立启动的子模块，更贴近真实微服务结构。
- **bootstrap 阶段读取**：Nacos Config 必须在 `bootstrap.yml` 配置，不能放 `application.yml`。
- **频率结论**：生产里 **服务注册发现 + 配置中心动态刷新** 是几乎必写；**多环境隔离、共享配置、元数据/权重** 按治理需要；**集群、加密** 属部署/安全范畴。
