# jdk8-sentinel-demo

> Sentinel 流量防卫兵学习 Demo：流控/熔断降级/热点参数/@SentinelResource 注解，含完整单元测试与 Dashboard 环境。

## 技术栈

| 分层 | 选型 | 说明 |
|------|------|------|
| 流量控制 | Sentinel 1.8.6 | spring-cloud-starter-alibaba-sentinel |
| Dashboard | bladex/sentinel-dashboard:1.8.6 | Docker 容器化运维面板 |
| 编程方式 | SphU.entry() + @SentinelResource | 程序化 + 注解，双轨演示 |

## 快速开始

```bash
# 1. 单元测试（无需任何外部依赖，规则由代码动态加载）
mvn -pl jdk8-sentinel-demo -am test

# 2. 完整 Sentinel Dashboard 体验（需要 Docker）
docker compose -f jdk8-sentinel-demo/docker-compose.yml up -d
# Dashboard 地址: http://localhost:8080，账号 sentinel/sentinel
mvn -pl jdk8-sentinel-demo -am spring-boot:run
# 触发几个请求后，在 Dashboard 界面可实时查看 QPS/RT 并动态编辑规则
```

## 场景一览

| # | 能力 | 场景文件 | 一句话 |
|---|------|---------|--------|
| FLOW-1 | QPS 直接限流 | `flow/FlowControlService#qpsLimited` | count=2 时高频调用触发限流，部分请求返回 blocked |
| FLOW-2 | WarmUp 预热 | `flow/FlowControlService#warmUpLimited` | 冷启动阈值缓慢爬升，高阈值(count=100)下正常通过 |
| FLOW-3 | WarmUp 低阈值 | 同上 | count=2 时预热期初始 QPS≈0.67，高频调用被限流 |
| FLOW-4 | 关联限流 | `flow/FlowControlService#refTrigger` + `#refProtected` | trigger 高 QPS 导致 ref 被连带限流 |
| DEG-1 | 异常捕获 | `degrade/DegradeService#exceptionRatio` | 业务异常走 catch 返回 fallback（未达熔断阈值） |
| DEG-2 | 正常通过 | `degrade/DegradeService#exceptionRatio` | 参数 throwEx=false 正常返回 passed |
| DEG-3 | 异常数熔断 | `degrade/DegradeService#exceptionCount` | 连续 3 次异常后熔断打开，后续请求直接返回 degraded |
| DEG-4 | 慢调用正常 | `degrade/DegradeService#slowCallRatio` | RT < 200ms 不触发慢调用熔断 |
| HOT-1 | 默认参数限流 | `hotspot/HotspotService#purchase` | 普通商品 QPS=2，高频调用被限流 |
| HOT-2 | VIP 参数不限流 | `hotspot/HotspotService#purchase` | 参数值 "vip" 享受 QPS=100 特权 |
| ANNO-1 | @SentinelResource fallback | `anno/SentinelAnnotationService#onlyFallback` | 只有 fallback，业务异常被捕获，限流则直接抛 BlockException |
| ANNO-2 | @SentinelResource blockHandler | `anno/SentinelAnnotationService#onlyBlockHandler` | 只有 blockHandler，限流走降级，业务异常直接抛出 |
| ANNO-3 | blockHandler+fallback 共存 | `anno/SentinelAnnotationService#bothHandlers` | 两者均配，限流与异常各有归宿，生产推荐方案 |
| ANNO-4 | 注解元数据验证 | 同上 | 运行时反射验证 @SentinelResource 参数正确性 |

## 场景详解

### 流控（Flow Control）

**三要素**：资源名（resource name）、阈值类型（FLOW_GRADE_QPS / FLOW_GRADE_THREAD）、流控效果（快速失败 / WarmUp / 排队等待）。

**直接限流**：对指定资源 QPS 做硬限制。`SphU.entry("qpsDemo")` 尝试获取通行证，超限抛 `BlockException`。

**关联限流**：典型场景——秒杀下单（被保护资源）与支付（触发者）共享读库。支付 QPS 过高时连带限制下单，保护数据库。

**WarmUp**：冷启动时阈值从 `count/3` 线性爬升到 `count`，避免刚启动的节点被瞬时流量打垮。适合网关/连接池初始化场景。

### 熔断降级（Degrade）

**状态机**：CLOSED → OPEN → HALF_OPEN → CLOSED（探测成功）或 OPEN（探测失败）。

|| 策略 | 触发条件 | 适用 |
||---|---|---|
|| 异常比例 | 异常占比 > 阈值 | 接口依赖外部服务，偶发超时可接受 |
|| 异常数 | 一分钟内异常数 > 阈值 | 关键接口不容任何异常 |
|| 慢调用比例 | RT 超阈值占比 > 阈值 | 数据库慢查询场景 |

**关键 API**：`Tracer.trace(ex)` 将异常上报给 Sentinel 统计——**只有被 trace 的异常才计入熔断决策**。

### 热点参数限流（Hotspot）

**核心思想**：不只看资源整体 QPS，而是按参数值做精细化控制。

```
默认: productId->QPS=2  (普通用户)
例外: productId="vip"->QPS=100  (VIP 白名单)
```

程序化方式通过 `SphU.entry(resourceName, entryType, count, args)` 传入参数值。

**生产场景**：单商品秒杀限流、单 IP 防刷、VIP/白名单用户豁免。

### @SentinelResource 注解

**三者分工**：

```
业务异常 → BlockException? 
  ├── 是 → blockHandler（限流专用处理）
  └── 否 → fallback（通用降级处理）
```

**blockHandler vs fallback 关键区别**：

|| | blockHandler | fallback |
|---|---|---|---|
|| 触发条件 | BlockException | 任何异常 |
|| 签名要求 | `(BlockException)` | `(Throwable)` |
|| 能否共存 | 是 | 是 |
|| 适用 | 限流/熔断触发时的兜底 | 任何未知异常的通用降级 |

**生产推荐**：两者均配，`blockHandler` 可主动调用 `fallback` 统一输出格式。

## 测试说明

单测通过 `@BeforeEach` 清空规则后各自加载，不依赖 Sentinel Dashboard。核心场景使用 `SphU.entry()` 程序化方式，比 `@SentinelResource` AOP 在单测中更稳定可控。

@SentinelResource 注解场景在单测中验证正常路径（无规则不触发限流）和注解元数据（`@SentinelResource` 参数正确性），完整的 blockHandler/fallback 回调行为需在 Dashboard 或实际限流规则触发下验证。

## 进阶方向

- **规则持久化**：默认规则存内存重启即失，生产应接入 Nacos/Apollo/ZooKeeper 动态规则源
- **链路流控**：通过对调用来源做隔离（SphU.entry 的 EntryType.IN 配合链路模式）
- **系统自适应限流**：基于 Load/CPU/RT 的全局自适应保护
- **Sentinel 网关限流**：集成 Spring Cloud Gateway + Sentinel 做入口统一限流
- **集群流控**：Token Server 模式，集群总 QPS 共享限制
- **规则动态推送**：Nacos DataSource 接入，实现 Dashboard 编辑→Nacos→应用热更新

## 设计要点

- **为什么核心场景用程序化而非注解**：`SphU.entry()` 不需要 Spring AOP 代理，单测环境更稳定；`try-catch` 在同一方法内就能写降级逻辑，学习链路清晰
- **为什么测试每个用例独立加载规则**：Sentinel 规则是 JVM 级全局状态，必须隔离避免交叉污染
- **`Tracer.trace(ex)` 的重要性**：Sentinel 靠它统计异常——直接抛异常不走 trace 不会被计入熔断决策
- **blockHandler 不等于 fallback**：blockHandler 只处理 BlockException，业务异常需要用 fallback 兜底
