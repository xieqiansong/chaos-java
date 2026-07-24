# jdk8-microservice-demo · 企业级微服务学习项目

> 一个贴近企业工程的 Spring Cloud Alibaba 微服务学习样例。形态为「多进程」：每个业务服务都是可独立部署 / 启动的进程。
> 开发计划与阶段拆解见 [`开发计划.md`](./开发计划.md)；每步进展见 [`开发日志.md`](./开发日志.md)。

---

## 1. 项目定位

| 维度 | 说明 |
|------|------|
| 学什么 | 用 Spring Boot + Spring Cloud Alibaba 搭一套**企业级微服务骨架**：模块分明、可观测、有安全认证、有分层测试 |
| 数据库主基调 | **PostgreSQL 作为默认 / 主数据源**；**MySQL 作为第二数据源**引入，演示**多数据源**配置与切换（你已熟悉 MySQL，故以 PG 为主学习重点） |
| 最想学的两块 | **熔断限流（Sentinel）** 与 **分布式事务（Seata）**——在基础模块就绪后**优先**落地 |
| 不做什么 | 不堆业务复杂度（仍是 user / order 这类教学模型）；不过度引入无关中间件 |

---

## 2. 技术栈

| 分类 | 组件 / 版本 |
|------|------|
| JDK | 1.8 |
| Spring Boot | 2.7.18 |
| Spring Cloud | 2021.0.9 |
| Spring Cloud Alibaba | 2021.0.6.2 |
| 注册 / 配置中心 | Nacos Server 2.2.3 |
| 网关 | Spring Cloud Gateway |
| 服务调用 | OpenFeign + Spring Cloud LoadBalancer |
| 熔断限流 | Sentinel（**优先学习**） |
| 分布式事务 | Seata AT 模式（**优先学习**） |
| 关系数据库 | PostgreSQL 16（主）/ MySQL 8（第二数据源），MyBatis-Plus + dynamic-datasource |
| 安全 | JWT（登录签发）+ 网关集中鉴权 |
| 可观测 | MDC traceId 全链路 + 访问日志切面 |
| 测试 | 多 profile + Testcontainers 起真实中间件 + JUnit5/Mockito |

---

## 3. 模块地图

### 3.1 common 层（非部署，纯依赖库，`lan.chaos.microservice.common.*`）
| 模块 | 职责 |
|------|------|
| `common-core` | 统一响应体 `R<T>`、业务异常 `BizException`、错误码、常量、分页对象、通用工具 |
| `common-log` | MDC `traceId` 工具、访问日志切面、logback 约定 |
| `common-web` | `@RestControllerAdvice` 全局异常、参数校验、`R` 响应包装、跨域 |
| `common-security` | JWT 工具（签发/校验）、`LoginUser` 上下文、`@RequiresPermission` 方法级权限注解 + 拦截器（网关与服务共用） |
| `common-feign` | Feign 自动配置：拦截器透传 `traceId`/`Authorization`、统一降级兜底 |
| `common-test` | 测试基类、Testcontainers 配置、H2 / 嵌入式中间件约定、造数工厂 |

### 3.2 业务服务层（可独立部署，`lan.chaos.microservice.*`）
| 模块 | 端口 | 职责 |
|------|------|------|
| `ms-gateway` | 8080 | 路由转发、集中鉴权、限流、链路起点 |
| `ms-auth` | 8083 | 登录签发 JWT（双令牌）、token 刷新、登出吊销（refreshToken 存 Redis，依赖 Redis） |
| `ms-user` | 8081 | 用户服务：注册 / 查询（主数据源 PostgreSQL） |
| `ms-order` | 8082 | 订单服务：创建 / 查询，Feign 编排调 user（演示跨服务 + Seata） |

---

## 4. 关键技术选型

- **多数据源（PostgreSQL 主 + MySQL 副）**：用 `dynamic-datasource-spring-boot-starter`，`@DS` 注解切换；默认 PostgreSQL，MySQL 命名为 `mysql`。每个业务服务配双数据源，Pg 承载主业务表，MySQL 演示异构数据源共存与事务边界。
- **可观测**：网关在请求入口生成 `X-Trace-Id` 写入 MDC；经 Feign 调用由 `common-feign` 透传；下游续接 MDC，保证一条请求跨进程同一 traceId。
- **安全**：`ms-auth` 登录签发**双令牌**（access 无状态短命 + refresh 存 Redis 可吊销）；`ms-gateway` 的 `AuthGlobalFilter` 用共享密钥本地验签、集中拦未认证请求；下游经 `PermissionInterceptor` 还原 `LoginUser` 并支持 `@RequiresPermission` 方法级校验。access/refresh 类型相互隔离，防越权。
- **熔断限流（Sentinel，优先）**：接入网关路由与 Feign 调用，做流控 / 熔断 / 降级，统一 `Fallback` 兜底防雪崩。
- **分布式事务（Seata AT，优先）**：`@GlobalTransactional` 跨服务，各库 `undo_log` 自动补偿；优先用「最终一致性」思路教学，Seata 作为强一致选项演示。
- **测试**：多 profile（dev/test/prod）；单测 JUnit5 + Mockito 无中间件 CI 必跑；集成测试 Testcontainers 起真实 PG/MySQL/Nacos；需中间件的用 `Assumptions` 探测可达性，不可达时优雅跳过。

---

## 5. 基础设施（docker-compose）

规划包含（按阶段启用，需要部署的服务会直接请你启动，不写模拟桩）：
- **Nacos 2.2.3**（8848/9848/9849）：注册发现 + 配置中心。
- **PostgreSQL 16**（5432）：主数据源业务库。
- **MySQL 8**（3306）：第二数据源，演示多数据源。
- **Sentinel Dashboard**（8858，优先阶段启用）：流控熔断可视化。
- **Seata Server**（8091，优先阶段启用）：分布式事务 TC。
- **Redis 7**（6379）：token 存储 / 限流计数 / 缓存。

---

## 6. 约定与规范

- **根包**：`lan.chaos.microservice`（common 子层加 `.common.*`，业务服务加 `.{service}`）。
- **命名**：启动类 `<Service>Application`；能力类见名知意；常量收 `common/constant` 或各服务 `common/constant`。
- **可观察**：每个对外接口必有「输入→输出」日志（traceId 串联），无静默执行。
- **测试**：每个场景至少一条可断言测试；需中间件的用 `Assumptions` 优雅跳过，禁止 `@Disabled`+`sleep` 当入口。
- **注释**：关键类 / 方法写 WHY（痛点 / 生产坑），保持本仓库讲解风格。

---

> 平台 `README.md` 学习记录表中本模块状态：**✅ P1/P2/P3/P4 已完成（企业级微服务骨架：多数据源 + 可观测 + 限流熔断 + 分布式事务 + 安全认证 全部落地）**。
