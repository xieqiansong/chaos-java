# 接口幂等（请求级 / 消费级 / 状态机级，统一去重框架）

> 用三层去重（请求级幂等号 / 消费级 messageId / 状态机级终态）统一演示「重复发生也不重复副作用」的正确性解法。
> 实体泛化为 `BizOrder` / `IdempotencyRecord`，无任何业务溯源语义。

## 1. 定位（一句话）

接口幂等不是「防止重复请求发生」，而是**在 at-least-once 投递语义下，重复发生了也不造成重复副作用**。本 Demo 用三层去重（请求级幂等号 / 消费级 messageId / 状态机级终态）统一演示这一正确性问题的解法，存储用 H2 内存去重表，开箱即跑。

## 2. 技术栈与入口

- **基线**：JDK 21 + Spring Boot 3.5.14
- **存储**：H2 内存库（零外部依赖），真实项目可替换为 MySQL/Redis
- **根包**：`lan.chaos.idempotent`
- **入口**：`IdempotentApplication` + `runner.DemoRunner`（控制台分节打印输入→输出）
- **触发**：`mvn test`（断言全部核心语义）/ `mvn spring-boot:run`（看主线演示）

## 3. 快速开始

```bash
# 单元测试（无需任何中间件）
mvn -pl jdk21-tech/jdk21-idempotent -am test

# 控制台主线演示
mvn -pl jdk21-tech/jdk21-idempotent spring-boot:run
```

预期输出（节选）：请求级并发双发 `副作用执行次数=1`；消费级 `实际消费次数=1`；状态机级 `重复回调=IGNORED`。

## 4. 场景一览

| 场景 | 类 | 去重键 | 一句话 |
|------|----|--------|--------|
| 请求级并发双发 | `demo.ConcurrentDoubleSubmitDemo` | requestId | 前端/网关超时重发，唯一约束保证仅一次副作用 |
| 消费级重复投递 | `demo.ConsumeDedupDemo` | messageId | MQ at-least-once 重发，消费侧去重 |
| 状态机级重复回调 | `demo.StateMachineDedupDemo` | bizNo(终态) | 上游重试回调，已终态忽略 |

## 5. 调用链路

三层防护共用同一条核心调用链，差异仅在「去重键来源」与「重复后的处理动作」：

```
调用方
  └─> Guard（RequestIdempotentGuard / ConsumeIdempotentGuard / StateMachineGuard）
        └─> IdempotencyStore.tryMarkFirst(record)       // 原子首检
              └─> H2IdempotencyStore（唯一约束 k+scope）
                    ├─ INSERT 行数 == 1  → 首检通过：执行真实业务副作用
                    └─ DuplicateKeyException → 首检失败：直接返回占位 / 跳过，绝不重复副作用

  * 请求级：requestId 来自客户端，首检与业务写在 @Transactional 同一事务内
  * 消费级：messageId 来自 broker，重复则直接 ack 跳过
  * 状态机级：bizNo 来自业务单，已终态则忽略（叠加内存终态表 + 去重表双保险）
```

首检原子性由数据库唯一约束保证，因此并发双发（两个相同 requestId 同时到达）下仍只放行一个事务。

## 6. 场景详解

### 6.1 请求级：并发双发正确兜底
- **关键 API**：`IdempotencyStore.tryMarkFirst` 用 `INSERT ... (k, scope) UNIQUE` 的受影响行数判定首检；冲突抛 `DuplicateKeyException` → 返回 `false`。
- **WHY（竞态坑）**：单线程「SELECT 再 INSERT」有竞态——两相同请求同时穿过 SELECT 都判首检成功，副作用执行两次。**唯一约束 + 受影响行数**把首检变成数据库原子操作，并发双发下仍只放行一个。
- **事务边界**：`RequestIdempotentGuard.execute` 用 `@Transactional` 把「首检 INSERT」与「业务写」包在同一事务，消除「首检成功但业务回滚、重复请求又进来」的窗口。

### 6.2 消费级
- 去重键来自 broker（messageId）而非客户端；重复投递直接 `return` 跳过，保证同事件只处理一次（避免重复扣款/通知）。

### 6.3 状态机级
- 以「业务单号 + 终态」为去重键：单子到达终态后，后续同单号回调直接忽略。比请求/消费级更细粒度——靠业务状态本身不可重复迁移天然去重，并叠加去重表双保险。

## 7. 进阶方向（生产化，未实现，学习路径）

1. **存储与过期**：去重表无限增长 → TTL 清理/归档/冷热分离，否则自身成瓶颈。
2. **跨库/跨服务**：多实例下用 Redis `SETNX` 或 DB 唯一约束做分布式首检；分布式锁（Redisson）可作补充。
3. **与最终一致性对账衔接**：幂等防「重复写」，对账防「漏写」——两者一起构成分布式写安全。去重表漏判（时序 bug）时靠对账兜底。
4. **请求级 vs 状态机级互补**：粗粒度去重表 + 细粒度状态机，重复维度分层防护。

## 8. 设计要点

- **三层去重维度**：请求级（客户端幂等号）/ 消费级（broker messageId）/ 状态机级（终态），对应不同故障来源。
- **原子首检是基石**：所有层共用 `H2IdempotencyStore.tryMarkFirst` 的「唯一约束 + 受影响行数」机制，竞态安全由数据库保证。
- **零外部依赖**：H2 内存库开箱即跑；换 MySQL/Redis 仅需替换 `IdempotencyStore` 实现，Guard 不动（符合最少依赖）。

## 目录

```
jdk21-idempotent/
├── pom.xml
├── README.md
└── src/
    ├── main/java/lan/chaos/idempotent/
    │   ├── IdempotentApplication.java
    │   ├── common/
    │   │   ├── constant/Scenario.java
    │   │   ├── model/IdempotencyRecord.java
    │   │   ├── model/BizOrder.java
    │   │   └── util/SampleFactory.java
    │   ├── config/IdempotentConfig.java
    │   ├── core/
    │   │   ├── IdempotencyStore.java
    │   │   ├── H2IdempotencyStore.java        # 唯一约束 + 受影响行数 原子首检
    │   │   ├── RequestIdempotentGuard.java    # 请求级 + @Transactional 并发双发兜底
    │   │   ├── ConsumeIdempotentGuard.java    # 消费级 messageId 去重
    │   │   └── StateMachineGuard.java         # 状态机级终态忽略
    │   ├── demo/
    │   │   ├── ConcurrentDoubleSubmitDemo.java
    │   │   ├── ConsumeDedupDemo.java
    │   │   └── StateMachineDedupDemo.java
    │   └── runner/DemoRunner.java
    └── test/.../{Request,Consume,StateMachine}GuardTest.java
```
