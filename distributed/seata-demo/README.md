# jdk8-seata-demo

> Seata 分布式事务学习 Demo：AT 自动补偿 + TCC 手动预留/确认/取消，含完整单元测试与 Docker 环境。

## 技术栈

| 分层 | 选型 | 说明 |
|------|------|------|
| 分布式事务 | Seata 1.6.x | spring-cloud-starter-alibaba-seata |
| 数据库 | H2（测试默认） / MySQL（docker-compose） | |
| ORM | Spring JdbcTemplate | 简单透明，聚焦 Seata 概念 |
| 连接池 | Druid | 父 POM 统一管理版本 |

## 快速开始

```bash
# 1. 单元测试（H2 内存数据库，无需任何外部组件）
mvn -pl jdk8-seata-demo -am test

# 2. 完整分布式事务体验（需要 Docker）
docker compose -f jdk8-seata-demo/docker-compose.yml up -d
mvn -pl jdk8-seata-demo -am spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://localhost:3306/seata_demo..."
# 然后通过测试或断点观察全局回滚行为
```

## 场景一览

> 标注「需 TC」的场景依赖真实 Seata Server（docker-compose），单测默认跳过，见「测试说明」。

### AT 模式

| # | 场景文件 | 一句话 |
|---|---------|--------|
| AT-1 | `at/BusinessService#purchase` | 正常购买：扣款→下单→扣库存，全部成功 |
| AT-2 | `at/BusinessService#purchaseFail` | 余额不足触发回滚，undo_log 反向补偿 |
| AT-3 | `at/AccountService` | 本地 @Transactional 回滚隔离 |
| AT-4 | `at/StorageService` | CAS 防超卖：WHERE total >= ? |
| AT-5 | `AtUndoLogTest` | undo_log 表结构 + "本地事务不生成镜像" |
| AT-6 | `AtUndoLogIntegrationTest` | 真实镜像写入 + 回滚补偿恢复（需 TC） |
| AT-7 | `GlobalLockConcurrencyTest` | 并发全局锁 + CAS 防超卖（需 TC） |

### TCC 模式

| # | 场景文件 | 一句话 |
|---|---------|--------|
| TCC-1 | `tcc/AccountTccAction` | 账户冻结→确认扣款→取消解冻 |
| TCC-2 | `tcc/OrderTccAction` | 订单预留(status=0)→确认(1)→取消(-1) |
| TCC-3 | `tcc/StorageTccAction` | 库存冻结预留→确认扣减→取消释放 |
| TCC-4 | `tcc/BusinessTccService` | 编排 Try/Confirm/Cancel 全流程 |
| TCC-5 | `TccSemanticsTest` | 三大问题语义：空回滚 / 幂等 / 悬挂防护 |

### SAGA / XA / XID

| # | 模式 | 场景文件 | 一句话 |
|---|------|---------|--------|
| SAGA-1 | SAGA | `saga/SagaBusinessService` | 补偿链编排：正向扣款→建单→扣库存 |
| SAGA-2 | SAGA | `SagaScenarioTest` | 首步失败无副作用 / 中间失败逆序补偿 |
| SAGA-3 | SAGA | `saga/config/SagaEngineConfig` + `SagaEngineIntegrationTest` | 官方状态机引擎（seata-saga-statelang + JSON）正向/补偿（需 TC） |
| XA-1 | XA | `xa/XaPurchaseService` | XA 业务代码形态（与 AT 完全一致） |
| XA-2 | XA | `common/config/XaDataSourceConfig` | XA 数据源代理切换点（需 TC + XA 库） |
| XID-1 | XID | `XidPropagationTest` | XID 绑定/解绑生命周期 + 跨线程传递 |

## 场景详解

### AT 模式（Automatic Transaction）

**原理**：Seata 拦截 DataSource，业务 SQL 执行前后自动记录"前镜像""后镜像"到 `undo_log` 表。回滚时根据 undo_log 反向生成补偿 SQL，开发者无需手写回滚代码。

**关键注解**：`@GlobalTransactional(timeoutMills=300000, rollbackFor=Exception.class)`

**执行流程**：
1. TM（BusinessService）向 TC 申请开启全局事务，获得 XID
2. 调用 AccountService → RM 注册分支事务、执行扣款、记录 undo_log
3. 调用 OrderService → RM 注册分支事务、创建订单、记录 undo_log
4. 调用 StorageService → RM 注册分支事务、扣库存、记录 undo_log
5. 全部成功 → TC 通知各 RM 提交（异步清理 undo_log）
6. 任一失败 → TC 通知各 RM 根据 undo_log 反向补偿回滚

**生产化要点**：
- undo_log 表需定期清理（建议按天分区 + 定时任务删 7 天前数据）
- 余额字段用 DECIMAL 而非 FLOAT/DOUBLE
- 高并发扣款用 CAS 写法：`UPDATE SET balance = balance - ? WHERE balance >= ?`

### TCC 模式（Try-Confirm-Cancel）

**原理**：开发者手动实现三个方法，Seata TC 协调调用顺序。Try 全部成功→Confirm，任一 Try 失败→已成功的执行 Cancel。

**三步曲对比**：

| 资源 | Try（预留） | Confirm（确认） | Cancel（取消） |
|------|------------|----------------|---------------|
| 账户 | frozen += amount | balance -= amount, frozen -= amount | frozen -= amount |
| 订单 | INSERT status=0 | UPDATE status=1 | UPDATE status=-1 |
| 库存 | frozen += count | total -= count, frozen -= count | frozen -= count |

**关键注解**：
- `@LocalTCC`：标记 TCC 接口
- `@TwoPhaseBusinessAction(name="xxx", commitMethod="commit", rollbackMethod="rollback")`：标记 Try 方法并指定二阶段回调
- `@BusinessActionContextParameter`：声明需传递到 Commit/Cancel 的参数

**幂等设计**：Confirm/Cancel 用 `WHERE frozen >= ?` 防止重复执行产生副作用。

**空回滚**：Try 因网络超时未执行但 TC 触发 Cancel 的情况，用 `WHERE frozen >= ?` 自然处理（frozen=0 时更新影响 0 行）。

**适用场景**：性能敏感、跨系统对接、需要精细控制资源的场景。

**三大经典问题（TccSemanticsTest 验证）**：
- **空回滚**：Try 因网络超时未执行但 TC 触发 Cancel，`WHERE frozen >= ?` 自然处理（frozen=0 时更新 0 行，不报错）
- **幂等**：Confirm/Cancel 因网络重试被重复调用，`WHERE frozen >= ?` 保证只生效一次
- **悬挂**：Confirm 成功后迟到的 Cancel 不得破坏已确认数据（frozen 已归零，幂等跳过）

### SAGA 模式（补偿链）

**原理**：每个参与者是**独立本地事务**，执行完立即提交（不持全局锁、不写 undo_log）；某步失败后，对**已成功执行**的步骤按**严格逆序**调用补偿方法。

**正向流程**：扣款 → 建单 → 扣库存；**失败补偿**：逆序撤销订单 → 加回余额（见 `saga/SagaBusinessService`）。

**与 AT/TCC 对比**：

| 维度 | AT | TCC | SAGA |
|------|----|----|------|
| 一致性 | 强一致（全局锁 + undo_log） | 最终一致（手动二阶段） | 最终一致（补偿链） |
| 一阶段 | 记录镜像、持全局锁 | 预留资源（frozen） | 直接提交本地事务 |
| 回滚 | undo_log 自动反向补偿 | Cancel 手动解冻 | 逆序调用补偿方法 |
| 适用场景 | 短事务 CRUD | 性能敏感 / 跨系统 | 长事务、无锁、容忍中间状态 |

**补偿设计要点**：
- 只补偿"已成功"的步骤（用 `done` 轨迹记录），避免把未执行的步骤也补偿导致数据错乱
- 补偿必须严格逆序（先撤销最后成功的步骤）

**官方状态机引擎（SAGA-3，`seata-saga-statelang`）**：

手写补偿链聚焦机制本身；生产化则用 Seata 官方引擎，以 JSON 声明状态机（`saga/stock_purchase.json`），由 `SagaEngineConfig` 装配（引擎/存储内置于 `seata-all`，无需额外依赖）：

- 正向：`DeductAccount` → `CreateOrder` → `DeductStorage`，每步声明 `CompensateState` 与 `Catch`（异常跳 `CompensationTrigger`）
- 失败：引擎自动将已完成步骤按逆序执行补偿（撤销订单 → 加回余额），实例状态置 `FA`
- 持久化：状态机定义/实例/状态分别落库 `seata_state_machine_def` / `_inst` / `seata_state_inst`（表结构见 `schema.sql`，与 1.6.1 源码一致）
- 支持分支、并行、超时、人工 forward / compensate（本 Demo 仅覆盖线性补偿链）

### XA 模式（强一致）

**原理**：业务代码与 AT **完全一致**（同样 `@GlobalTransactional`），区别仅在数据源代理与数据库支持。

**关键差异**：

| 维度 | AT | XA |
|------|----|----|
| 一致性 | 最终一致（undo_log 补偿） | 强一致（数据库 XA 协议） |
| 一阶段 | 记录镜像 + 全局锁，本地提交 | 各库 Prepare，全局 Commit / Rollback |
| 锁 | Seata 全局锁 | 数据库原生行锁（提交前长期持有） |
| 适用场景 | 大部分业务 | 强一致、事务短、可接受持锁 |

**启用 XA（见 `common/config/XaDataSourceConfig`）**：
1. 数据源改用 XA 代理：`new DataSourceProxyXA(rawDataSource)`
2. 关闭 starter 自动 AT 代理：`seata.enable-auto-data-source-proxy: false`
3. 数据库需支持 XA 协议（MySQL InnoDB / Oracle / H2 支持）

### XID 传递

**原理**：XID 是 Seata 全局事务的唯一标识，必须随调用链路传播。`RootContext` 基于 ThreadLocal 持有 XID，跨线程/线程池不会自动继承，需显式透传并重新 `bind`（见 `XidPropagationTest`）。

- 同线程：`@GlobalTransactional` 自动 bind/unbind
- 跨线程（`@Async`/线程池）：任务提交前取 XID → 子线程内重新 `RootContext.bind(xid)` → 执行后 `unbind()`
- 跨 RPC（Feign/RestTemplate）：Seata 自动在请求头透传
- 跨 MQ：需在消息头携带 XID，消费端手动恢复

## 测试说明

单测环境下 `seata.enabled=false`（见 `src/test/resources/application.yml`），不连接 Seata Server。此时：

- `@GlobalTransactional` 不生效，各 Service 的 `@Transactional` 独立工作
- TCC Commit/Cancel 不会被框架自动回调（`TccSemanticsTest` 直接手工调用二阶段方法验证语义）
- SAGA 补偿链、XID 传递、undo_log 表结构均为纯本地可验证场景，单测直接覆盖
- 测试聚焦：业务逻辑正确性、异常传播、Try 阶段预留语义、TCC 三大问题、补偿链、XID 传播

**需真实 Seata Server 的场景**（`AtUndoLogIntegrationTest`、`GlobalLockConcurrencyTest`、`SagaEngineIntegrationTest`）用 `Assumptions.assumeTrue` 优雅跳过：

```bash
# 1. 启动 Seata Server + 可选 MySQL
docker compose -f jdk8-seata-demo/docker-compose.yml up -d

# 2. 带真实 TC 运行集成场景（AT 镜像回滚、全局锁并发、SAGA 官方引擎）
mvn -pl jdk8-seata-demo -am test -Dseata.enabled=true
```

## 进阶方向

- ~~SAGA / XA~~（已覆盖：手写补偿链 + XA 代理配置，见上）
- **多数据源**：在 Demo 中为每个"微服务"分配独立 DataSource
- **Nacos 注册中心**：Seata TC/TM/RM 通过 Nacos 发现
- **TCC 幂等增强**：用状态机 + 数据库唯一键替代 frozen 字段判断
- ~~SAGA 官方引擎~~（已覆盖：`SagaEngineConfig` + `saga/stock_purchase.json` + `SagaEngineIntegrationTest`）
- **SAGA 引擎高级特性**：分支/并行/子状态机/超时/人工 forward & compensate（当前仅覆盖线性补偿链）

## 设计要点

- **为什么 AT 模式不抛异常不会回滚**：Seata 只对 RuntimeException 触发回滚，返回错误码会被视为成功
- **为什么 TCC Try 不直接扣款**："预留→确认→取消"模型将业务操作与事务协调解耦，Confirm/Cancel 可幂等重试
- **为什么用 JdbcTemplate 而非 MyBatis-Plus**：Demo 聚焦 Seata 事务概念，简单 SQL 足够，避免 ORM 干扰学习主线
- **H2 MODE=MySQL**：让 H2 模拟 MySQL 语法（如 MERGE、反引号），一套 schema.sql 兼容测试和生产
