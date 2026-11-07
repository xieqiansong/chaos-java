# 开发设计文档：finance-demo（金融支付清结算与账务核心 · 综合实战）

> 来源：DeepSeek 对话《从纯技术环境切换到金融领域》提炼（金融系统版图 / 核心业务概念 / 技术思维映射）。
> 定位：`chaos-java` 的 `showcase/` 综合实战 demo（用户显式要求新增，见 `AGENTS.md` 第七章）。
> 目标：用**一个纵向业务闭环**把对话里的高频工程约束落成可运行、可观察、可断言的代码，并贯穿金融四条底线——**数据准确性、一致性、安全性、可审计性**。

---

## 0. 设计原则（底线优先）

对话反复强调：金融软件里这四条是不可妥协的底线，性能优化必须让位。本 demo 每个能力包都必须能演示至少一条底线：

| 底线 | 在本 demo 的落点 |
|---|---|
| 数据准确性 | `money`（BigDecimal 防浮点）+ `ledger`（借贷必平衡、总分勾稽） |
| 一致性 | `settlement`（TCC 最终一致）+ `reconciliation`（对账兜底强一致） |
| 安全性 | `audit`（敏感数据脱敏 + 审计留痕）+ 复用 `crypto-demo` 思路 |
| 可审计性 | `ledger` append-only 流水 + `audit` 不可篡改操作日志 |

防御性思维（对话结尾三问）作为统一编码纪律，写进每个涉资金/状态类的 WHY 注释：
**① 被并发调用两次会怎样？② 中途失败会留下什么中间状态？③ 消息重复消费会怎样？**

---

## 1. 需求对齐（从对话提炼）

### 1.1 金融系统版图 → 本 demo 聚焦

| 域 | 是否覆盖 | 对应能力包 | 说明 |
|---|---|---|---|
| 核心银行（账户/总账/日终） | ✅ 部分 | `ledger` + `batch` | 账户分层、复式记账、日终批处理 |
| 交易支付引擎（状态机/幂等/记账/对账） | ✅ 主线 | `payment` + `ledger` + `reconciliation` | 本 demo 的业务外壳 |
| 清算 / 结算（净额/全额、日终对账） | ✅ | `settlement` + `reconciliation` | 轧差、清算文件、勾对 |
| 风控 / 合规（审计/脱敏/权限分离） | ✅ 轻量 | `audit` | 脱敏、审计留痕、RBAC 概念 |
| 证券 / 信贷 / 资管 | ❌ 进阶方向 | — | 见第 10 章，本期不做 |

### 1.2 核心业务概念 → 落地为能力包

| 业务概念（对话原文） | 能力包 | 演示要点 |
|---|---|---|
| 复式记账 Double-Entry | `ledger` | 每笔变动≥2 科目，借贷必平衡 |
| 账户分层（客户→分户账→总账） | `ledger` | 分户账余额 = 总账科目汇总，勾稽一致 |
| 冲正 Reversal | `ledger` | 异常不删原单，生成反向分录抵消 |
| 幂等性 Idempotency | `payment` | 业务流水号唯一键 + 乐观锁 + 状态机前置校验 |
| 状态机 State Machine | `payment` | 非法跃迁拒绝（如待支付直跳退款） |
| 最终一致性（TCC/可靠消息/对账补偿） | `settlement` | Try 冻结 / Confirm 扣减 / Cancel 冲正 |
| 对账 Reconciliation | `reconciliation` | 渠道 vs 本方勾对、长款/短款/金额不符 |
| 日终批处理（日切/分片/断点续跑/不重不丢） | `batch` | 总分核对、分片续跑、幂等重跑 |
| 审计留痕 / 敏感数据保护 | `audit` | append-only 日志、卡号/证件/手机号脱敏 |

### 1.3 技术思维映射 → 与 chaos-java 既有模块（避免重复，明确复用）

| 对话提到的技术点 | 既有模块（参考/复用） | 本 demo 的差异 |
|---|---|---|
| 幂等 | `distributed/idempotent-demo`（三层去重） | 金融强约束版：资金防重，状态机兜底 |
| 分布式事务（Seata/TCC） | `distributed/seata-demo` | 手写最小 TCC，零框架依赖演示语义 |
| 加密 / 签名 / 脱敏 | `crypto/crypto-demo`、`crypto/hmac-auth-demo` | 仅做脱敏工具，算法引用既有模块 |
| 一致性算法 | `distributed/consensus-demo` | 对账兜底思想，非共识算法 |
| 定时任务 / 批处理 | `engineering/scheduler-demo` | 日终分片 + 断点续跑 |
| MQ 不丢不重顺序性 | `mq/kafka-demo`、`mq/rocketmq-demo` | 内存通道模拟，标注生产替换点 |

---

## 2. 业务闭环与架构

### 2.1 纵向故事线（一笔支付的完整链路）

```
[1 下单] payment 创建 TradeOrder(INIT)，生成业务流水号
   ↓
[2 支付] payment 状态机 INIT→PAYING，幂等校验（同流水号只处理一次）
   ↓  （模拟 MQ：支付成功事件，标注真实用 RocketMQ/Kafka 的替换点）
[3 记账] ledger 生成复式凭证：客户账户借/贷记 + 过渡科目，借贷平衡，append-only 流水
   ↓
[4 清算] settlement TCC：Try 冻结头寸 → Confirm 实扣（或 Cancel 冲正）
   ↓
[5 日终] batch 日切：总分核对（分户账汇总 == 总账科目），分片续跑
   ↓
[6 对账] reconciliation 解析渠道对账文件，与本方流水勾对，差异分类（长/短款）
   ↓
[贯穿] audit 记录每步操作（不可篡改）+ 敏感字段脱敏
```

### 2.2 包结构（综合实战允许业务分层，但每个能力包仍满足 `AGENTS.md` 第三章）

```
showcase/finance-demo/
├── pom.xml                      # SB2(release 8) / spring-boot-starter / lombok / spring-boot-starter-test
├── DESIGN.md                    # 本设计文档
├── README.md                    # 七段式
└── src/
    ├── main/java/lan/chaos/finance/
    │   ├── FinanceApplication.java
    │   ├── common/
    │   │   ├── constant/   Currency / OrderStatus / ClearingStatus / DiffType
    │   │   ├── model/      Money / Direction / Account / Subject / Voucher / JournalEntry
    │   │   │              TradeOrder / ClearingBill / Position / ReconRecord
    │   │   ├── store/      AccountStore / SubjectStore / VoucherStore / PaymentOrderStore
    │   │   │              PositionStore / ClearingBillStore
    │   │   ├── exception/ UnbalancedVoucherException / ReconcileException
    │   │   │              IllegalStateTransitionException / ClearingException
    │   │   └── runner/     MoneyDemoRunner / LedgerDemoRunner / PaymentDemoRunner
    │   │                   SettlementDemoRunner / ReconciliationDemoRunner / BatchAndAuditDemoRunner
    │   ├── money/           MoneyArithmetic
    │   ├── ledger/          DoubleEntryEngine / ReversalService / ReconcileLedger / LedgerSamples
    │   ├── payment/         TradeStateMachine / PaymentService / PayResult / PaymentSamples
    │   ├── settlement/      TccClearing / NettingService / SettlementSamples
    │   ├── reconciliation/  ReconParser / ReconMatcher / DiffHandler
    │   ├── batch/           WorkItem / CheckpointStore / ShardedRunner / DayEndJob
    │   └── audit/           MaskUtils / AuditEntry / AuditLogger
    └── test/java/lan/chaos/finance/
        ├── money/           MoneyArithmeticTest
        ├── ledger/          LedgerTest
        ├── payment/         PaymentTest
        ├── settlement/      TccClearingTest / NettingServiceTest
        ├── reconciliation/  ReconMatcherTest
        ├── batch/           DayEndJobTest
        ├── audit/           AuditTest
        └── flow/            FinanceE2EIT（*IT，verify 阶段）
```

> 约定（贴合 `AGENTS.md`）：根包 `lan.chaos.finance`；能力包是顶层（`money/ledger/...`）；`controller`/触发外壳收进 `common/trigger/`；样例数据用 `sampleXxx()` 工厂；每能力一个类讲清一个点 + WHY 注释 + 输入→输出可观察。

### 2.3 运行形态

- **单进程 + H2 内存库**：零外部中间件，CI 自包含（`mvn test` 直接跑）。
- **MQ 模拟**：支付成功事件用进程内 `Channel`/同步调用模拟，代码里用 `// TODO 生产替换：RocketMQ/Kafka，保证不丢不重顺序性` 标注，引用 `mq/` 既有 demo。
- **可选 HTTP 触发**：`common/trigger/` 提供少量端点供 curl 把玩；核心演示靠单元测试 + 控制台 Runner，不依赖 Web。

---

## 3. 数据模型草图（实体 / 表）

| 实体 | 关键字段 | 说明 |
|---|---|---|
| `Money` | `amount(BigDecimal)`、`currency`、`scale`、`roundingMode` | 值对象，不可变；构造只接受 String/long，拒绝 double |
| `Subject`（科目/总账） | `code`、`name`、`balance` | 机构级核算，借贷方向 |
| `Account`（分户账） | `id`、`ownerId`、`subjectCode`、`balance`、`version` | 客户看到的余额；乐观锁 version 防并发 |
| `Voucher`（凭证） | `id`、`bizNo`(业务流水号)、`entries` | 一笔交易的借贷分录集合 |
| `JournalEntry`（分录） | `voucherId`、`subjectCode`、`direction(DEBIT/CREDIT)`、`money` | append-only，不可更新/删除 |
| `TradeOrder`（交易订单） | `id`、`bizNo`、`status`、`amount`、`payerId`、`payeeId`、`version` | 状态机驱动 |
| `ClearingBill`（清算单） | `id`、`netAmount`、`type(NET/GROSS)`、`status` | 轧差结果 |
| `ReconRecord`（对账记录） | `id`、`channelBizNo`、`localBizNo`、`diffType(NONE/LONG/SHORT/MISMATCH)` | 差错分类 |
| `AuditLog`（审计日志） | `id`、`operator`、`action`、`target`、`maskedDetail`、`ts` | 不可篡改，敏感字段已脱敏 |

> 表结构由 JPA/H2 或手动 DDL 在测试里建；本 demo 偏内存演示，不追求生产级 schema。

---

## 4. 各能力包设计（一个类一个点 + WHY）

### 4.1 `money` — 金额与货币运算（★ 地基）
- `Money` 值对象：`of(String)`/`of(long,scale)`；运算 `add/subtract/multiply/divide` 返回新实例；`compareTo`；`round(mode)`。
- WHY：浮点误差在资金场景不可接受；double 构造会引入精度损，必须 String/long 入参。

### 4.2 `ledger` — 复式记账引擎
- `DoubleEntryEngine`：`post(Voucher)` 校验「借贷合计相等」否则抛 `UnbalancedException`；更新分户账余额（乐观锁）；写 append-only 分录。
- `ReversalService`：冲正 = 生成反向凭证（原借变贷），不删原单。
- `ReconcileLedger`：分户账汇总 == 总账科目，勾稽断言。
- WHY：账务核心的「准确性 + 可审计性」；任何资金变动可追溯、可平衡校验。

### 4.3 `payment` — 交易状态机 + 幂等
- `TradeStateMachine`：枚举 `INIT/PAYING/PAID/FAILED/REFUNDED`；`transition()` 拒绝非法跃迁（如 INIT→REFUNDED）。
- `PaymentService`：`bizNo` 唯一键 + 乐观锁 version + 状态机前置校验，并发重复请求只生效一次。
- WHY：金融厌恶重复扣款；状态机比 if-else 拼状态可靠（对话原话）。

### 4.4 `settlement` — TCC 清算轧差
- `TccClearing`：`try()` 冻结头寸 → `confirm()` 实扣 → `cancel()` 冲正；演示最终一致性。
- `NettingService`：多边应收应付轧差（net amount），对比全额（gross）。
- WHY：跨服务强一致代价高，转向 TCC/可靠消息 + 对账补偿（对话「最终一致性」）。

### 4.5 `reconciliation` — 对账
- `ReconParser`：解析固定宽度/CSV 渠道对账文件（样例数据 `sampleChannelFile()`）。
- `ReconMatcher`：本方流水 vs 渠道逐笔勾对，分类 `NONE/LONG/SHORT/MISMATCH`。
- `DiffHandler`：自动冲正钩子（演示，不接真实差错流程）。
- WHY：系统间定时比对，差异进差错处理（对话「对账」）。

### 4.6 `batch` — 日终批处理
- `DayEndJob`：日切 + 总分核对；`ShardedRunner` 分片 + 断点续跑（记录 checkpoint），保证「不重不丢」。
- WHY：联机 + 批量双模式；分片/重试/续跑是高频实战题（对话「时间与批量」）。

### 4.7 `audit` — 审计留痕 + 脱敏
- `AuditLogger`：append-only 操作日志，不可篡改。
- `MaskUtils`：卡号/身份证/手机号脱敏（`****1234`），日志严禁打印完整敏感信息。
- WHY：审计留痕 + 敏感数据保护是合规硬要求（对话「合规与安全」）。

---

## 5. 接口 / 触发形态

- **首选**：每个能力包自带 `*Test`（断言语义 + 可观察输出）；纯库部分提供 `DemoApp.main()` 分节打印「输入→输出」。
- **可选**：`common/trigger/` 少量 HTTP 端点（curl 把玩支付→记账→对账），不含知识点。
- 不靠 `@Disabled`+`sleep` 当入口；无外部依赖直接跑，有依赖用 `Assumptions` 跳过。

---

## 6. 测试清单（`*Test` / `*IT` / `*Bench`）

| 场景 | 类型 | 断言要点 |
|---|---|---|
| Money 构造/运算/舍入 | `*Test` | double 构造被拒；加减小数精度正确；除不尽按模式舍入 |
| 借贷不平衡被拒 | `*Test` | `post` 抛 `UnbalancedException` |
| 正常记账后余额与流水一致 | `*Test` | 分户账余额 == 分录汇总 |
| 冲正抵消 | `*Test` | 反向凭证后净额为 0，原单保留 |
| 并发重复支付只扣一次 | `*Test` | 多线程同 `bizNo` 仅一次生效 |
| 非法状态跃迁被拒 | `*Test` | INIT→REFUNDED 抛异常 |
| TCC cancel 冲正 | `*Test` | try 后 cancel，头寸回滚 |
| 轧差净额正确 | `*Test` | 多边互冲后 net == 预期 |
| 对账差异全检出 | `*Test` | 构造长/短/不符样例全部分类正确 |
| 总分勾稽 | `*Test` | 分户账汇总 == 总账科目 |
| 脱敏正确 | `*Test` | 卡号中间位被遮，前后保留 |
| 端到端链路 | `*IT` | 下单→支付→记账→清算→日终→对账 全流程跑通（H2，零外部） |
| 并发支付吞吐 | `*Bench`（可选） | 吞吐/P99，落 `bench-results/`，不含凭证 |

---

## 7. 非功能底线落点（防御性思维三问）

每个涉资金/状态类顶部 WHY 注释回答三问：
1. **并发两次**：`payment` 唯一键 + 乐观锁；`ledger` 余额更新带 version。
2. **中途失败**：`settlement` TCC 的 cancel 冲正；`batch` checkpoint 续跑，不重不丢。
3. **消息重投**：幂等 `bizNo` 去重；对账兜底强一致（即使 MQ 重投也不重复记账）。

---

## 8. 工程规范与落地清单（实现时执行）

- [ ] `pom.xml`：挂 `chaos-bom-springboot2`（SB 2.7.18，release 8）；引入 `spring-boot-starter-test`、`h2`、`spring-batch`（可选）；版本走 BOM，不写死。
- [ ] 根包 `lan.chaos.finance`；启动类 `FinanceApplication`；能力包顶层，触发外壳进 `common/trigger/`。
- [ ] 每个能力类：`sampleXxx()` 样例 + WHY 注释 + 输入→输出打印 + 至少一条 `*Test`。
- [ ] README 七段式（第六章形态）：定位 / 技术栈入口 / 快速开始 / 场景一览 / 场景详解 / 进阶方向 / 设计要点；在根 `README.md` 归「综合实战」并标注「可选·用户显式新增」。
- [ ] `TECH-INDEX.md`：综合实战表新增 `finance-demo` 行，注明「用户显式新增（原约束为保留不新增）」。
- [ ] 敏感信息只走 `application-local.yml`（已被 gitignore），bench 报告不落凭证/内网地址。

---

## 9. 实施顺序（roadmap）

| 阶段 | 内容 | 依赖 |
|---|---|---|
| P0 | `money` + `common/model/util` 地基 | 无 |
| P1 | `ledger`（复式记账/冲正/总分勾稽） | money |
| P2 | `payment`（状态机+幂等） | ledger |
| P3 | `settlement`（TCC/轧差）+ `reconciliation`（对账） | payment/ledger |
| P4 | `batch`（日终）+ `audit`（审计/脱敏）+ 端到端 `*IT` + README/INDEX 同步 | 其余 |

---

## 10. 进阶方向（不在本期，README 进阶章节列出）

- 信贷（额度/还款计划/五级分类）、证券（撮合/T+1）、资管（估值/TA）。
- 真实 MQ（RocketMQ/Kafka）落地「不丢不重顺序性」；Seata 框架版 TCC。
- 权限分离（SoD）RBAC/ABAC 完整实现；国密 SM2/SM4 接入资金传输。
- 多机构 / 跨行清算通道（大额/小额/网联银联）模拟。

---

## 11. 选型结论（已与用户确认）

| 问题 | 结论 |
|---|---|
| 业务外壳粒度 | 「单笔支付 → 记账 → TCC 清算 → 对账 → 日终 → 审计」纵向切片，由 `FinanceE2EIT` 串成闭环 |
| `settlement` 方案 | **手写最小 TCC**（零依赖，含幂等 / 空回滚 / 防悬挂），框架版引用既有 `seata-demo` |
| `batch` 方案 | **手写分片 + checkpoint 断点续跑**（更轻、零额外依赖），不引入 `spring-batch` / H2 |
| 落点与命名 | `showcase/finance-demo`（用户显式要求新增综合实战），标题「金融支付清结算与账务核心」 |

### 落地与设计的差异（均为「更轻、更聚焦」）

- **存储**：设计稿写 H2；实际按 `AGENTS.md` 5.3「内存/内嵌优先」全部用内存 Store，`mvn test` 零外部依赖、CI 自包含，接口保持不变可替换。
- **触发外壳**：设计稿预留 `common/trigger/` HTTP 端点；实际用 `common/runner/` 的多个 `ApplicationRunner` 控制台分节打印，未引 `spring-boot-starter-web`，避免 Web 噪音。
- **工具层**：`MoneyUtils` / `SeqGenerator` / `Json` 未单独建类（`Money` 自带运算、`bizNo` 由调用方传入），避免过度设计；`MaskUtils` 归入 `audit` 包。
- **综合实战定位**：由用户显式要求新增，已在根 `README.md` 与 `TECH-INDEX.md` 标注，不违反「保留不新增」的默认约束。

### 并发正确性加固（实现后复查发现并修复）

代码全部落地后，针对「资金正确性」做了一轮并发复查，发现两个真实缺陷（均已修复并有回归测试）：

| # | 缺陷 | 后果 | 修复 |
|---|---|---|---|
| 1 | `TccClearing.tryFreeze` 把幂等检查放在 `synchronized` 之外 | 两线程同时看到 `existing==null`，各自冻结一次，但 `putIfAbsent` 只落一张单 → **冻结 200、只释放 100，永久挂账** | Try/Confirm/Cancel 共用 `synchronized (billStore)`，把「幂等检查 + 头寸变动 + 单据写入」收进同一临界区；空回滚标记同锁 |
| 2 | `DoubleEntryEngine.post` 的 `getBalance().add(delta)` 是 read-modify-write | 热点账户并发记账**丢失更新**，余额凭空少算 | 整笔凭证在 `synchronized (accountStore)` 内原子完成（等价数据库事务+行锁）；`ReconcileLedger` 共用同锁，避免读到撕裂快照 |

**为什么不能逐账户 CAS**：一笔凭证同时改多个分户账与总账，逐账户 CAS 会产生「部分成功」，破坏复式记账的原子性；生产对应一个数据库事务，或用 `UPDATE ... WHERE version=?` 乐观锁 + 冲突重试。

**回归测试**：`TccClearingTest`（32 线程重复 Try / 16 线程重复 Confirm）、`LedgerConcurrencyTest`（64 线程精确累加、并发记账期间勾稽不撕裂）。已做「临时还原旧逻辑 → 确认测试变红 → 立即恢复」的反向验证，排除假通过；单测连跑 3 轮稳定。

### 验证结果

```
mvn -o -pl showcase/finance-demo -am verify
→ 40 单测（surefire） + 1 端到端 IT（failsafe）全部通过，BUILD SUCCESS
```
