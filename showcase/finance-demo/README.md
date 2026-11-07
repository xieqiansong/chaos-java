# finance-demo · 金融支付清结算与账务核心（综合实战）

## 1. 一句话定位

用一条「单笔支付 → 复式记账 → TCC 清算 → 对账 → 日终 → 审计」的纵向业务闭环，把金融软件的四条底线——**数据准确性、一致性、安全性、可审计性**——落成可运行、可观察、可断言的最小可用 demo。综合实战（用户显式要求新增），把多个技术点组合进带业务外壳的项目。

## 2. 技术栈与入口类、触发方式

| 项 | 值 |
|---|---|
| 构建主线 | SB 2.7.18 / `release 8`（挂 `chaos-parent-jdk8` + import `chaos-dependencies-spring-boot2`） |
| 外部依赖 | **零**：纯 JDK + Spring Boot 核心（非 Web），全部内存存储 |
| 根包 | `lan.chaos.finance` |
| 启动类 | `FinanceApplication`（`@SpringBootApplication`，非 Web） |
| 触发方式 | ① `mvn -o -pl showcase/finance-demo -am test` 跑全部单测；② `mvn -o -pl showcase/finance-demo -am verify` 追加端到端 `*IT`；③ `mvn -o -pl showcase/finance-demo spring-boot:run` 看各能力包 `ApplicationRunner` 的控制台「输入→输出」演示 |
| 设计文档 | [`DESIGN.md`](DESIGN.md)（需求对齐、领域地图、技术思维映射） |

控制台演示触发器在 `common/runner/`，用 `@Order(1..6)` 保证按 `money → ledger → payment → settlement → reconciliation → batch/audit` 的「由浅入深」顺序分节打印，**不含知识点**，仅用于把玩。

> Windows PowerShell 下中文输出若乱码，先执行 `chcp 65001` 切到 UTF-8 再启动；这是终端编码问题，不影响代码与测试。

## 3. 快速开始

```bash
# 在 chaos-java 根目录
mvn -o -pl showcase/finance-demo -am test      # 40 个单测
mvn -o -pl showcase/finance-demo -am verify    # 40 单测 + 1 端到端 IT
mvn -o -pl showcase/finance-demo spring-boot:run   # 控制台演示
```

预期（`test`）：

```
Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

预期（`verify` 追加）：

```
Running lan.chaos.finance.flow.FinanceE2EIT
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

控制台演示片段：

```
===== [money] 金额运算演示（P0 地基） =====
[double] 0.1 + 0.2 = 0.30000000000000004  ← 误差！
[Money] 0.1 + 0.2 = CNY 0.30  ← 精确
...
===== [ledger] 复式记账演示（P1） =====
转账后 A=CNY -100.00 B=CNY 100.00 总账=CNY 0.00
总分勾稽：通过
冲正后 A=CNY 0.00 B=CNY 0.00 总账=CNY 0.00（净额归零，原单保留）
...
===== [payment] 状态机 + 幂等演示（P2） =====
首次支付: PayResult{order=BIZ-PAY-001, status=PAID, duplicated=false}  → 完成真实扣款
重复支付: PayResult{order=BIZ-PAY-001, status=PAID, duplicated=true}   → 幂等命中，未再扣款
真实扣款次数 = 1（应为 1）
```

## 4. 场景一览

按「由浅入深」排序，每个能力包 = 一个知识点（根包 `lan.chaos.finance.<capability>`）：

| # | 场景 | 能力包 | 触发/验证 | 一句话 |
|---|---|---|---|---|
| 0 | 金额运算 | `money` | `MoneyArithmeticTest` / `MoneyDemoRunner` | BigDecimal 防浮点误差，币种与精度约束 |
| 1 | 复式记账 | `ledger` | `LedgerTest` / `LedgerDemoRunner` | 借贷必平衡、append-only 分录 |
| 2 | 冲正 | `ledger` | `LedgerTest#冲正后净额归零且原单保留` | 异常不删原单，生成反向凭证抵消 |
| 3 | 总分勾稽 | `ledger` | `LedgerTest#分户账被篡改时总分勾稽报错` | 分户账汇总 == 总账科目 |
| 4 | 交易状态机 | `payment` | `PaymentTest#状态机规则表可观察` | 白名单迁移，非法跃迁拒绝 |
| 5 | 幂等支付 | `payment` | `PaymentTest#并发重复支付只扣一次`（32 线程） | 唯一键 + 状态前置 + 对象锁 |
| 6 | 中途失败 | `payment` | `PaymentTest#中途失败落到FAILED而非悬挂PAYING` | 不留下悬空的中间态 |
| 7 | TCC 清算 | `settlement` | `TccClearingTest` | Try 冻结 / Confirm 实扣 / Cancel 解冻，含幂等·空回滚·防悬挂 |
| 8 | 清算轧差 | `settlement` | `NettingServiceTest` | 全额 230 → 净额 70，只划净额 |
| 9 | 对账 | `reconciliation` | `ReconMatcherTest` | 文件解析 + 长款/短款/金额不符分类 |
| 10 | 日终批处理 | `batch` | `DayEndJobTest` | 分片 + 断点续跑，不重不丢 |
| 11 | 审计与脱敏 | `audit` | `AuditTest` | append-only 日志 + 卡号/证件/手机号脱敏 |
| 12 | 并发资金正确性 | `ledger` / `settlement` | `LedgerConcurrencyTest` / `TccClearingTest`（32/64 线程） | 热点账户不丢更新、Try 不重复冻结、Confirm 不重复入账 |
| 13 | 全链路 | `flow` | `FinanceE2EIT`（`verify`） | 下单→日终，资金守恒且只记一次账 |

## 5. 场景详解

### money · 金额（输入 → 输出 → 关键 API → 坑）

- 输入 `Money.of("0.1", CNY)` + `Money.of("0.2", CNY)` → 输出 `CNY 0.30`；`double` 对照得到 `0.30000000000000004`。
- 关键 API：`Money.of(String)` / `ofMinor(long)`（以"分"构造）/ `add`/`subtract`/`divide(RoundingMode)` / `compareTo`。
- **坑**：构造入口绝不接 `double`；不同币种直接运算抛 `IllegalArgumentException`；除不尽必须显式给舍入模式。

### ledger · 复式记账 / 冲正 / 勾稽

- 输入：一笔 A→B 转账 100（A 贷 100、B 借 100）→ 输出：A `-100`、B `+100`、总账 `0`。
- 关键 API：`DoubleEntryEngine.post(Voucher)`（不平衡抛 `UnbalancedVoucherException`）、`ReversalService.reverse(voucherId)`、`ReconcileLedger.assertConsistent()`。
- **坑**：分录必须 append-only；异常走冲正而非删除；绕过引擎直接改分户账会被勾稽抓出。
- **并发坑（易漏）**：`getBalance().add(delta)` 是 read-modify-write，两个线程读到同一旧值各加一次只会生效一次 → **丢失更新**，热点账户余额凭空少算。修复是整笔凭证在 `synchronized (accountStore)` 内原子完成（等价数据库「事务 + 行锁」）。注意不能逐账户 CAS：一笔凭证涉及多账户，会破坏「要么全成要么全败」。`ReconcileLedger` 也共用这把锁，否则日终核对会读到「分户已改、总账未改」的撕裂快照。见 `LedgerConcurrencyTest`（64 线程断言精确累加 64 次）。

### payment · 状态机 + 幂等

- 状态白名单：`INIT→PAYING/FAILED`、`PAYING→PAID/FAILED`、`PAID→REFUNDED`，`FAILED`/`REFUNDED` 是终态。
- 输入：同一 `bizNo` 并发 32 次 → 输出：仅 1 次真实扣款，其余 `duplicated=true`，返回同一订单同一终态。
- 三层防护：`PaymentOrderStore.putIfAbsent`（唯一键）→ 状态机前置校验（仅 INIT 可支付）→ `synchronized(order)` 串行化。
- **坑**："待支付直接退款"会被拒绝；扣款中途抛错会落到 `FAILED`，不悬挂在 `PAYING`。

### settlement · TCC + 轧差

- TCC 输入：A/B 各 1000 头寸，A 划 100 给 B → Try 后 A `可用900/冻结100`（守恒），Confirm 后 A `900`、B `1100`（全局守恒 2000）。
- **TCC 三大坑**：Confirm/Cancel 幂等；Cancel 先到的「空回滚」打标记；迟到 Try 被「防悬挂」拒绝。
- 轧差输入：A→B 100、A→B 50、B→A 80 → 全额 `230.00`、净额 `70.00`，实际只划一笔 `A→B 70.00`。
- **坑**：单机构 `total()` 只在 Try 阶段守恒，Confirm 后要看全局之和。
- **并发坑（资损级）**：幂等检查若放在锁外，两个线程会同时看到 `existing == null`、各自冻结一次，而 `putIfAbsent` 只让一张单生效 → **冻结 200 但只释放 100，剩余 100 永久挂账**。修复是让 Try/Confirm/Cancel 共用 `synchronized (billStore)`，把「幂等检查 + 头寸变动 + 单据写入」做成原子临界区；空回滚标记也必须在同一把锁内，否则与迟到的 Try 交叉。见 `TccClearingTest#并发重复Try只冻结一次且Confirm后无挂账`（32 线程），该用例已用「临时还原旧逻辑」验证过确实会失败，不是假通过。

### reconciliation · 对账

- 输入：渠道文件（含表头/空行）与本方流水 → 输出：`NONE=1, LONG=1, SHORT=1, MISMATCH=1`，差错动作 3 条。
- 关键 API：`ReconParser.parseCsv`、`ReconMatcher.match/countByType`、`DiffHandler.handle`。
- **坑**：金额一律 `new BigDecimal(String)`；解析要容错空行/表头/空白。

### batch / audit · 日终与合规

- 日终输入：5 条流水分 3 片，在 D3 注入宕机 → 第一轮处理 D1/D2，第二轮跳过 D1/D2 只补 D3/D4/D5，最终「无重复、无遗漏」。
- 审计输入：卡号 `6222021234567890` → 输出 `************7890`；快照 `clear()` 抛 `UnsupportedOperationException`。
- **坑**：checkpoint 必须在「处理成功后」才标记；日志明细写入前强制脱敏。

## 6. 进阶方向（生产化考量，即使未实现也列出）

- **MQ 不丢不重顺序性**：本 demo 用内存/同步模拟事件。生产替换点见 `PaymentService` 与 `TccClearing` 注释，参考本仓库 `mq/kafka-demo`、`mq/rocketmq-demo`。
- **分布式事务框架**：这里手写最小 TCC；框架版见 `distributed/seata-demo`（AT/TCC/SAGA/XA，含空回滚/幂等/悬挂）。
- **真实持久化**：内存 Store 换成 JPA/H2 + 数据库唯一键约束 + 乐观锁 `version`；`application-local.yml` 放连接串（已被 gitignore）。
- **权限分离（SoD）与 RBAC/ABAC**：参考 `microservice/security-demo`、`oauth2-demo`；审计落独立只追加库或哈希链防篡改。
- **国密与字段加密**：参考 `crypto/crypto-demo`（SM2/SM3/SM4）、`crypto/hmac-auth-demo`。
- **其他金融域**：信贷（额度/还款计划/五级分类）、证券（撮合/T+1/涨跌停）、资管（估值/TA）、风控（规则引擎/Flink 实时指标）。
- **批处理框架**：用 Spring Batch 的 `JobRepository` 替代手写 checkpoint，获得重试/跳过/重启策略。

## 7. 设计要点

**为什么这么拆**：能力包按「由浅入深」的认知顺序组织，正好对应实现路线 P0→P4。地基 `money` 无依赖，`ledger` 依赖 `Money`，`payment` 复用状态机与幂等，`settlement`/`reconciliation` 建立在前两者之上，`batch`/`audit` 收口，最后由 `FinanceE2EIT` 串成闭环。

**为什么用内存存储**：本 demo 的知识点是「资金不变式」而非持久化框架。按 `AGENTS.md` 5.3「内存/内嵌优先」，内存 Store 让 `mvn test` 零外部依赖、CI 自包含；所有 Store 接口保持不变，替换成 JPA/H2 不影响能力包逻辑。

**四条底线落点**：准确性（`Money` + 借贷平衡 + 总分勾稽）、一致性（幂等 + TCC + 对账兜底）、安全性（脱敏）、可审计性（append-only 分录与审计日志）。

**并发正确性（本课重点）**：对话的防御性三问里，第一问「被并发调用两次会怎样」最容易只做表面幂等。本 demo 三处各自用了不同手法，正好可对照学习：`payment` 用「唯一键 + 状态前置 + 对象锁」、`ledger` 用「凭证级临界区」（多账户必须整体原子）、`settlement` 用「三阶段共用同一临界区」（幂等检查必须在锁内）。三者都有多线程回归测试，且 TCC 那条验证过「还原旧逻辑必失败」。

**与既有模块的关系**：不重复造轮子——幂等对照 `distributed/idempotent-demo`、分布式事务对照 `distributed/seata-demo`、加解密对照 `crypto/*`、批处理对照 `engineering/scheduler-demo`。本模块聚焦「金融业务语义下的正确性约束」。

**工程细节**：`spring-boot-maven-plugin` 设了 `<classifier>exec</classifier>`——`repackage` 默认会用 fat jar 替换主 artifact，导致 `verify` 阶段 failsafe 找不到 `BOOT-INF/classes` 下的业务类（`NoClassDefFoundError`）；加 classifier 后可执行 jar 单独产出，主 jar 保持普通 jar。

> 完整设计（需求对齐 / 领域地图 / 技术思维映射 / 数据模型 / 实施顺序）见 [`DESIGN.md`](DESIGN.md)。
