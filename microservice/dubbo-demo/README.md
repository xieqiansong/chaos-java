# dubbo-demo

Apache Dubbo RPC 框架演示模块，覆盖**服务暴露（`@DubboService`）/ 服务引用（`@DubboReference`）/ 注册中心发现（Nacos）/ 异步调用 / 集群容错 / 负载均衡**六大核心能力。按「接口先行」的 RPC 协作方式组织：provider 实现接口并导出，consumer 引用接口并跨进程调用。

- 基础包：`lan.chaos.dubbo`
- 技术栈：Apache Dubbo 3.2.10 + Spring Boot 2.7.18 + Nacos 2.3.2（注册中心）+ JDK 8
- 触发入口：
  - **零依赖控制台演示**：`DemoApp.main`（单进程点对点直连，覆盖全部场景，CI 必跑）
  - **真实多进程形态**：先 `docker-compose up -d` 起 Nacos，再起 `ProviderApplication` + `ConsumerApplication`（带 `nacos` profile）
  - **验证**：`DubboScenarioTest`（直连，零外部依赖）/ `ConsumerRegistryIT`（Nacos，可达才跑）

> 使用频率标注：`★★★ 高频`／`★★☆ 中频`／`◆ 基础`（契约/配置模块，非独立业务场景）。

## 快速开始

```bash
# 1) 零依赖跑全部场景（单进程点对点直连，无需任何中间件）
mvn -pl microservice/dubbo-demo -am test          # DubboScenarioTest 断言四个核心机制
# 或在 IDE 直接运行 DemoApp.main 看「输入→输出」分节打印

# 2) 真实多进程 + 注册中心形态（需 Docker）
cd microservice/dubbo-demo && docker-compose up -d   # 起 Nacos(8848)
mvn -pl microservice/dubbo-demo/dubbo-provider -am spring-boot:run
mvn -pl microservice/dubbo-demo/dubbo-consumer  -am spring-boot:run -Dspring-boot.run.profiles=nacos
# ConsumerApplication 启动后会自动调用一次并打印跨进程结果
```

## 目录结构

```
dubbo-demo/
├── pom.xml                      # 聚合父 pom：定版 Dubbo（BOM 未管理，注释说明）
├── docker-compose.yml           # Nacos 2.3.2 standalone（注册中心）
├── dubbo-api/                   # ◆ 共享契约（接口 + 模型 + 常量）
│   └── lan/chaos/dubbo/api
│       ├── UserService.java                 # RPC 接口（同步 + 异步方法）
│       └── common/{constant,model}          # DubboConstants / User(可序列化)
├── dubbo-provider/              # 服务提供者（真实多进程形态） ★★★
│   └── lan/chaos/dubbo/provider
│       ├── ProviderApplication.java         # @EnableDubbo，导出 + 注册 Nacos
│       └── UserServiceImpl.java             # @DubboService 实现
│   └── application.yml                      # dubbo://:20880 + nacos 注册中心
└── dubbo-consumer/              # 服务消费者 ★★★
    ├── lan/chaos/dubbo/consumer/real        # 真实多进程形态（@DubboReference 走注册中心）
    │   ├── ConsumerApplication.java
    │   └── UserFacade.java
    └── lan/chaos/dubbo/consumer/demo        # 单进程点对点演示（零依赖）
        ├── DemoApplication.java / DemoApp.java（控制台 Runner）
        ├── DemoConfig.java                  # 单个 dubbo 协议(20880)，各 provider 用 group 区分
        ├── provider/                        # Basic/Async/Flaky/LbA/LbB 五个内联 provider
        └── facade/                          # Basic/Async/Cluster/LoadBalance 四个 facade
```

> 设计要点：provider / consumer 是**角色单元**（形态二），真实形态经 Nacos 跨进程；`demo` 包把 provider+consumer 放进同一 JVM 直连，用最少噪音讲清机制，且零外部依赖可测。

## 场景一览（按使用频率排序）

`★★★ 高频`
- [服务接口契约 api](#1-服务接口契约-api-基础) → 接口先行，两端共用
- [服务暴露 `@DubboService`](#2-服务暴露-dubboservice-高频) → provider 把 Bean 导出为远程服务
- [服务引用 `@DubboReference`](#3-服务引用-dubboreference-高频) → consumer 拿到动态代理，调本地写法
- [注册中心发现 Nacos](#4-注册中心发现-nacos-高频) → 注册→发现→跨进程调用全链路

`★★☆ 中频`
- [异步调用 CompletableFuture](#5-异步调用-completablefuture-中频) → 方法返回 Future，调用线程不阻塞
- [集群容错 cluster](#6-集群容错-cluster-中频) → failover/failfast/failsafe/forking/broadcast
- [负载均衡 loadbalance](#7-负载均衡-loadbalance-中频) → random/roundrobin/leastactive/consistenthash

`◆ 基础模块`
- [dubbo-api 共享契约](#1-服务接口契约-api-基础)

---

### 1. 服务接口契约 api `◆`

RPC 协作的基础：provider 与 consumer 都只依赖 `UserService` 接口，不依赖彼此实现。接口放独立模块 `dubbo-api`，编译期解耦。

| 类 | 说明 |
|----|------|
| `api/UserService.java` | RPC 接口：`getById` / `list`（同步）+ `getByIdAsync`（异步，返回 `CompletableFuture`） |
| `api/common/model/User.java` | 传输模型，实现 `Serializable`（Hessian2 序列化必需） |
| `api/common/constant/DubboConstants.java` | 协议名 / 端口 / Nacos 地址 / 服务名，杜绝魔法值 |

---

### 2. 服务暴露 `@DubboService` `★★★`

把普通 Spring Bean 暴露成远程服务。Dubbo 启动时生成 exporter，监听 `dubbo://:20880` 并把元数据注册到 Nacos；consumer 拿到的是动态代理。

- 关键属性：`version` / `group`（灰度隔离）、`timeout`、`retries`、`weight`。
- 生产坑：模型必须可序列化；接口要稳定（用专门 RPC DTO，别透传业务实体）；避免超大参数/返回值。

| 类 | 说明 |
|----|------|
| `provider/UserServiceImpl.java` | `@DubboService` 实现，返回带实例标识的样例用户 |
| `provider/ProviderApplication.java` | `@EnableDubbo` 开启扫描并连 Nacos |

验证：见 `DubboScenarioTest.basic_syncCall_returnsSampleUser`。

---

### 3. 服务引用 `@DubboReference` `★★★`

在 consumer 端生成指向远程服务的代理。字段写本地接口，调用被 Dubbo 拦截、序列化、经 Netty 发到 provider、反序列化返回——对业务透明。

- 不写 `url` → 走注册中心发现；写 `url="dubbo://host:port"` → 点对点直连（见 `demo` 包）。
- 关键属性：`check`（启动是否强依赖 provider）、`timeout`/`retries`、`loadbalance`、`cluster`。

| 类 | 说明 |
|----|------|
| `real/UserFacade.java` | `@DubboReference`（无 url，走 Nacos 发现） |
| `demo/facade/BasicUserFacade.java` | `@DubboReference(url=...)` 直连演示 |

验证：见 `DubboScenarioTest.basic_syncCall_returnsSampleUser` / `ConsumerRegistryIT.discoveryViaNacos_thenInvoke`。

---

### 4. 注册中心发现 Nacos `★★★`

provider 注册实例、consumer 订阅实例列表——这是 Dubbo 相对「写死地址」的核心价值：实例扩缩容、故障剔除对调用方透明。本 demo 用 Nacos（与 `nacos-demo` 同栈）。

- 配置：`dubbo.registry.address=nacos://127.0.0.1:8848`
- 多实例时，consumer 内置客户端负载均衡从列表选地址（见场景 7）

触发：起 Nacos → 起 Provider → 起 Consumer（`--spring.profiles.active=nacos`），观察 `ConsumerApplication` 打印的跨进程结果。

验证：见 `ConsumerRegistryIT`（Nacos 可达时才执行，否则优雅跳过）。

---

### 5. 异步调用 `CompletableFuture` `★★☆`

consumer 拿到 `CompletableFuture`，调用线程不必阻塞等网络，可先做别的事再 `.get()` 或 `.thenAccept(...)`。高并发下减少线程等待、提升吞吐。Dubbo 以「方法返回 Future」识别异步，无需额外配置。

| 类 | 说明 |
|----|------|
| `provider/demo/AsyncProvider.java` | `getByIdAsync` 用 `supplyAsync` 异步构造结果 |
| `consumer/demo/AsyncUserFacade.java` | 引用后 `.get()` 取结果 |

验证：见 `DubboScenarioTest.async_call_returnsFutureResult`。

---

### 6. 集群容错 `cluster` `★★☆`

分布式调用必遇 provider 偶发失败。Dubbo 在 consumer 端内置多种集群策略（由 `cluster` 指定）：

- `failover`（默认）：失败重试其他实例，`retries` 控制次数；
- `failfast`：快速失败不重试（写操作）；
- `failsafe`：失败忽略（日志/统计）；
- `forking`：并行发多个、任一成功即返回；`broadcast`：广播全部。

本 demo 用 `failover,retries=2,timeout=500ms` + 前两次阻塞超时的 `FlakyProvider`，演示「重试后恢复」；返回 `age` 字段承载累计尝试次数（3），可观察。

| 类 | 说明 |
|----|------|
| `provider/demo/FlakyProvider.java` | 前 2 次阻塞超过 500ms 触发超时，第 3 次成功 |
| `consumer/demo/ClusterFaultTolerantFacade.java` | `@DubboReference(cluster="failover", retries=2, timeout=500)` |

验证：见 `DubboScenarioTest.cluster_failover_retriesThenRecovers`。

---

### 7. 负载均衡 `loadbalance` `★★☆`

同一服务多实例时，consumer 端决定「这次请求发给谁」，由 `loadbalance` 指定：`random`（默认，按权重）、`roundrobin`（轮询）、`leastactive`（最活跃优先）、`consistenthash`（一致性哈希）。真实形态下实例列表来自注册中心（Nacos），Dubbo 内置 `loadbalance` 直接在实例列表上做选择；单 JVM 点对点演示无法在同一接口上挂两个注册实例，故 demo 用两个独立 group 实例（lbA / lbB，均导出在 20880）等价模拟「两个实例」并在 consumer 侧交替调用，观察请求被分摊到 A/B。

| 类 | 说明 |
|----|------|
| `provider/demo/LoadBalanceProviderA.java` / `B.java` | 实例标识 A / B（group=lbA / lbB，均导出在 20880） |
| `consumer/demo/LoadBalanceFacade.java` | `@DubboReference(group="lbA"/"lbB", loadbalance="roundrobin")` 交替调用收集命中分布 |

验证：见 `DubboScenarioTest.loadBalance_roundrobin_hitsBothInstances`。

---

## 如何运行

```bash
# 零依赖演示（推荐先看这个）
mvn -pl microservice/dubbo-demo -am test            # 断言四大核心机制
# IDE 运行 DemoApp.main 看分节「输入→输出」

# 真实多进程 + Nacos
docker-compose up -d                                # microservice/dubbo-demo 下
mvn -pl microservice/dubbo-demo/dubbo-provider -am spring-boot:run &
mvn -pl microservice/dubbo-demo/dubbo-consumer  -am spring-boot:run -Dspring-boot.run.profiles=nacos
```

## 进阶方向（生产考量，未写成独立 Demo）

- `◆` **Triple 协议（Dubbo3）**：新一代协议，天然支持流式、跨语言（gRPC 兼容）、HTTP/2，相比 dubbo 协议更适合云原生多语言互通。
- `◆` **泛化调用 / 隐式参数**：`GenericService` 无需接口类即可调用；`RpcContext` 传递隐式附件（链路追踪、鉴权）。
- `◆` **服务分组 / 版本灰度**：`group` + `version` 做同接口多套实现与灰度发布。
- `◆` **Mock 与降级**：`mock="return null"` / 自定义 Mock 类，provider 不可用时快速失败或返回兜底。
- `◆` **可观测**：Dubbo Admin 看服务治理、Metrics 接入 Micrometer/Prometheus。

## 设计要点

- **零依赖优先**：用「单进程点对点直连」把四个核心机制讲清并可断言（`DubboScenarioTest`），真实多进程 + Nacos 形态由 `ConsumerRegistryIT` 在可达时验证——外部依赖严格走 docker-compose + 可达性跳过。
- **接口先行 + 契约独立**：`dubbo-api` 只放接口/模型/常量，provider/consumer 共用，编译期解耦。
- **形态二（多进程）**：provider / consumer 按角色拆模块、各一个 `<Role>Application`，还原 Dubbo 真实运行方式；`demo` 包不污染真实形态。
- **频率结论**：服务暴露/引用/注册发现是几乎必写；异步、集群容错、负载均衡是生产化必需的高频配置。
