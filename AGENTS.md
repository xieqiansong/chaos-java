# AGENTS.md — A 类 Demo 生成规范（硬约束）

本仓库只把 **A 类「单技术点 Demo」** 作为核心交付物，用本规范约束 AI 生成的每一个 demo。
**B 类「综合实战 Demo」**（如 `jdk8-seckill-demo` / `jdk8-short-link-demo`）仅为面试/落地演示，**不受本规范约束、不投入额外设计，少量保留即可**。

> 新生成的 A 类 demo 必须以本仓库的 **`jdk8-localcache-demo`（Caffeine 本地缓存）为模板**：目录结构、注释风格、测试形态、README 七段式全部照抄。

---

## 一、A 类 Demo 的定位

- **一个 Demo = 一个技术组件 / 一个技术点**（Redis、RocketMQ、MapStruct、本地缓存…）。
- 目标只有一条：**让看的人以最少噪音、最快速度抓住该技术点的关键机制**。
- 不是「能跑的小项目」，不是「微服务骨架」，不是「完整业务系统」。

## 二、目录与分包（强制）

- **根包固定为 `lan.chaos.<tech>`**（如 `lan.chaos.redis`、`lan.chaos.localcache`）。**禁止** `lan.chaos.demo.<tech>` 这种带 `demo` 层的写法。
- **能力即顶层包**：`lan.chaos.<tech>.<capability>`，每个 capability 聚焦一类知识点、一个场景一个类。
  - 正面：`cache/basic`、`cache/expire`、`cache/eviction`、`lock`、`simple`、`order`、`basic`、`collection`…
  - 反面：`controller`、`service`、`repository`、`dao`、`dto` 占顶层包——**一律禁止**，这些只是触发外壳或支撑，收进 `common/`。
- **支撑与触发统一收进 `common/`**：
  - `common/constant/`：topic / key / 缓存名 / 端口等公共常量，杜绝魔法值。
  - `common/config/`：Spring 配置类（如 `CacheConfig`）。
  - `common/model/`：样例数据模型（如 `User`）。
  - `common/trigger/`（可选）：HTTP 端点等「触发外壳」，仅当需要用 curl/Postman 交互式把玩时才加，**非必须**。
- 启动类命名 `<Tech>Application`（如 `LocalCacheApplication`），放在 `lan.chaos.<tech>` 根下。

## 三、场景代码规范（强制）

- **一个场景一个类**：类/方法单一职责，命名见名知意（`BasicCacheService`、`OrderProducer`、`DistributedLock`）。
- **必须自带样例数据**：提供 `sampleXxx()` 工厂造默认数据，调用方无需自己准备输入。
- **必须可观察**：场景执行后务必打印/返回「输入 → 输出」（日志、JSON、控制台），**静默执行是零分**。
- **注释讲 WHY 而非只写 WHAT**：每个能力类必须有一段说明——该机制解决什么痛点、关键 API 是什么、生产环境有什么坑/差异。
- **外部依赖最小化**：能用内存/JDK/Embedded 实现的（Caffeine、H2、EmbeddedKafka、MapStruct 编译期）就**不要**引真实中间件。必须连外部组件时，提供 `docker-compose.yml` + 合理默认地址，README 写明「先起组件再起 Demo」。

## 四、验证与触发形态（强制，首选顺序）

1. **单元测试（首要形态，及格线）**：每个场景至少一条可断言的 `*Test`，既验证语义又充当「可观察输出」。无外部依赖时直接跑；CI 无外部依赖时靠 `Assumptions` 优雅跳过。**禁止**用「`@Disabled` 整体禁用 + `Thread.sleep` 当入口」的做法（那是 RocketMQ demo 的历史包袱，新 demo 不得复制）。
2. **控制台 Runner（可选）**：纯库/非 Web 想分节打印「输入→输出」时，提供 `DemoApp.main()`，用分隔线把每个场景的输出分节打印。
3. **HTTP 端点（可选）**：仅当需交互式把玩才加，放 `common/trigger`，且不含知识点。
4. **场景注册表（可选，非强制）**：把场景抽象为 `Scenario { name(); description(); run(); }` 由 Spring 收集，经 `ApplicationRunner` 或测试加载，支持「按名 / 一键全跑」。

> 关键认知：**Demo 的好坏不靠 Web 控制器撑着**。没有 HTTP 端点，单元测试 + 控制台输出同样能把每个场景讲清楚、跑通、可观察。

## 五、README 七段式（强制）

每个 A 类 Demo 的 README 必须包含：
1. **一句话定位**（学什么、覆盖哪些能力）
2. **技术栈与入口类、触发方式**
3. **快速开始**（docker-compose 如需 + 启动命令 + 验证命令/预期输出）
4. **场景一览**（按使用频率/难度排序，每条带触发方式 + 一句话解释）
5. **场景详解**（输入输出、关键 API、坑点）
6. **进阶方向**（生产化考量，即使未实现也列出，指明学习路径）
7. **设计要点**（为什么这么拆、频率结论）

完成后把所在平台 `README.md` 的「已完成学习记录」表对应状态更新为 ✅。

## 六、B 类综合实战（仅记录，不约束）

- 允许分层（controller/service/repository/dto/config），但**不要求**对齐上述 A 类规则。
- 在平台 `README.md` 中单独归类为「综合实战（锦上添花）」，明确标注「可选、不强制统一规则」。
- 不新增 B 类 demo，除非用户明确要求。

---

## ⛳ AI 生成 A 类 Demo 自检清单（生成后逐条核对）

新建或生成 A 类 demo 后，必须全部满足，否则视为不合格：

- [ ] 根包为 `lan.chaos.<tech>`，无 `demo` 中间层
- [ ] 能力是顶层包（`<tech>.<capability>`），`controller/service/repository/dto` 未占顶层
- [ ] 公共常量在 `common/constant`，配置在 `common/config`，样例模型在 `common/model`
- [ ] 每个能力一个类，命名见名知意，且含 `sampleXxx()` 样例数据
- [ ] 每个能力类有 WHY 注释（痛点 / 关键 API / 生产坑）
- [ ] 每个场景执行后有「输入→输出」可观察输出，无静默执行
- [ ] 至少一条可断言的 `*Test`；无外部依赖直接跑，有依赖用 `Assumptions` 跳过；**无** `@Disabled`+`sleep` 当入口
- [ ] 非必要不引真实中间件；必须引时附 `docker-compose.yml` + README 说明
- [ ] README 含七段式，且平台总表状态置 ✅
- [ ] 已参考 `jdk8-localcache-demo` 的目录与风格（不得自创一套）
