# AGENTS.md — 怎样算一个好 Demo

本文件是**整个仓库**（`jdk8-platform` 及后续 `jdk11/17/21/25-platform`）的 Demo 编写规范。
只讲**标准与原则**，不规定构建工具的具体操作步骤（那些 AI 自然知道）。
目标：让每个放进本仓库的 Demo 都「能上手、能玩、能学」。

> 当前实质内容集中在 `jdk8-platform`，下文标杆与示例均取自该目录；其余平台沿用同一套原则。

---

## 一、质量标杆

本仓库已有两位标杆（位于 `jdk8-platform`），新 Demo 请以它们为参照：

- **`jdk8-platform/jdk8-rocketmq-demo`**：按能力子包拆分（`simple` / `order` / `delay` / `batch` / `transaction` …），每场景一个 `@Service` 类、职责单一；抽象出公共能力（`MessageUtils` 信封、`MessageIdStore` 去重、`IdempotentConsumer` 幂等）。注释讲清 **WHY**：例如 `IdempotentConsumer` 说明 RocketMQ at-least-once、用 `msgId` 去重、生产环境应换 Redis/DB 唯一键。
- **`jdk8-platform/jdk8-nacos-demo`**：贴近真实微服务的多子模块拆分（`common` / `provider` / `consumer` / `config`）；服务发现与配置中心两大能力；配置监听同时给出 `@RefreshScope` 自动刷新与编程式 `ConfigListener` 两种形态，并列出「热更新线程池 / 限流规则 / 本地缓存」等适用场景。

> 共同特质：**场景分层清晰、公共能力抽象、可观察输出、注释解释设计取舍与生产化考量**。

---

## 二、好 Demo 的两道及格线

1. **上手友好**：克隆即编译、一条命令启动、外部依赖尽量少；有 README 告诉用户「怎么跑、看到什么」。
2. **场景可加载**：每个学习点都是**一个独立、可触发、可观察的场景**，并有统一入口去运行它（而不是埋在某个 main 里只有作者会调）。

---

## 三、场景设计原则（核心）

- **一个场景一个文件**：类/方法单一职责，命名见名知意（`CacheUserScenario`、`OrderProducer`）。
- **自带样例数据**：提供 `sampleXxx()` 工厂造默认数据，用户无需自己准备输入。
- **必须可观察**：场景执行后务必打印/返回「输入 → 输出」（日志、JSON、控制台），**静默执行是零分**。
- **按能力分包**：`lan.chaos.xxx.<capability>`（如 `cache` / `lock` / `rank`），每包聚焦一类知识点；必要时像 rocketmq 那样抽出 `common` 放公共工具。
- **常量前置**：topic / key / 端口等定义在本类或 `constant` 包顶部，避免魔法值。
- **外部依赖最小化**：必须连外部组件（Redis / Kafka / Nacos）时，提供 `docker-compose.yml` + 合理默认地址，README 写明「先起组件再起 Demo」。
- **解释 WHY 而非只写 WHAT**：注释/README 要讲清设计取舍、踩坑点、生产化差异（参考上面两个标杆的注释风格）。

---

## 四、场景加载：几种形态（参考，不强制）

| 形态 | 适用 | 示例 |
|------|------|------|
| HTTP 端点 | 有外部组件 / Web 交互 | `redis-demo` 的 `/redis/cache`、`/redis/lock` |
| 控制台 Runner | 纯库 / 非 Web | `jdk8-mapstruct-demo` 的 `DemoApp.main()` 分节打印 |
| 单元测试 | 任何可断言逻辑 | `jdk8-mapstruct-demo` 的 `*Test` |
| 场景注册表 | 场景多、想「按名 / 一键全跑」 | 见下 |

可选「场景注册表」思路（概念，非强制模板）：把场景抽象为 `Scenario { name(); description(); run(); }`，由 Spring 自动收集进 `ScenarioRegistry`，再经 HTTP 或 `ApplicationRunner` 加载。Web 类 Demo 推荐「HTTP 端点触发」，库类推荐「控制台 Runner + 测试」。

---

## 五、README 约定

每个 Demo 的 README 不要只罗列目录，要教用户「怎么玩」，建议包含：

1. 一句话定位（学什么、覆盖哪些能力）
2. 技术栈与入口类、触发方式
3. 快速开始（docker-compose 如需 + 启动命令 + 验证命令/预期输出）
4. 场景一览（按使用频率/难度排序，每条带触发方式 + 一句话解释）
5. 场景详解（输入输出、关键 API、坑点）
6. 进阶方向（生产化考量，即使未实现也列出，指明学习路径）
7. 设计要点（为什么这么拆、频率结论）

完成一个 Demo 后，记得把所在平台的 `README.md`（如 `jdk8-platform/README.md`）「已完成学习记录」表中对应状态更新为 ✅。
