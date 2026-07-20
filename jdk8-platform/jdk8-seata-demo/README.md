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

| # | 模式 | 场景文件 | 一句话 |
|---|------|---------|--------|
| AT-1 | AT | `at/BusinessService#purchase` | 正常购买：扣款→下单→扣库存，全部成功 |
| AT-2 | AT | `at/BusinessService#purchaseFail` | 余额不足触发回滚，undo_log 反向补偿 |
| AT-3 | AT | `at/AccountService` | 本地 @Transactional 回滚隔离 |
| AT-4 | AT | `at/StorageService` | CAS 防超卖：WHERE total >= ? |
| TCC-1 | TCC | `tcc/AccountTccAction` | 账户冻结→确认扣款→取消解冻 |
| TCC-2 | TCC | `tcc/OrderTccAction` | 订单预留(status=0)→确认(1)→取消(-1) |
| TCC-3 | TCC | `tcc/StorageTccAction` | 库存冻结预留→确认扣减→取消释放 |
| TCC-4 | TCC | `tcc/BusinessTccService` | 编排 Try/Confirm/Cancel 全流程 |

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

## 测试说明

单测环境下 `seata.enabled=false`（见 `src/test/resources/application.yml`），不连接 Seata Server。此时：

- `@GlobalTransactional` 不生效，各 Service 的 `@Transactional` 独立工作
- TCC Commit/Cancel 不会被框架自动回调
- 测试聚焦：业务逻辑正确性、异常传播、Try 阶段预留语义

完整分布式事务行为（AT 全局回滚、TCC 自动 Cancel）需通过 docker-compose 启动 Seata Server 后验证。

## 进阶方向

- **SAGA 模式**：长事务编排，正向补偿 + 逆向补偿链
- **XA 模式**：强一致性，依赖数据库 XA 协议
- **多数据源**：在 Demo 中为每个"微服务"分配独立 DataSource
- **Nacos 注册中心**：Seata TC/TM/RM 通过 Nacos 发现
- **TCC 幂等增强**：用状态机 + 数据库唯一键替代 frozen 字段判断

## 设计要点

- **为什么 AT 模式不抛异常不会回滚**：Seata 只对 RuntimeException 触发回滚，返回错误码会被视为成功
- **为什么 TCC Try 不直接扣款**："预留→确认→取消"模型将业务操作与事务协调解耦，Confirm/Cancel 可幂等重试
- **为什么用 JdbcTemplate 而非 MyBatis-Plus**：Demo 聚焦 Seata 事务概念，简单 SQL 足够，避免 ORM 干扰学习主线
- **H2 MODE=MySQL**：让 H2 模拟 MySQL 语法（如 MERGE、反引号），一套 schema.sql 兼容测试和生产
