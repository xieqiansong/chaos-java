# AGENTS.md — Demo 生成规范（硬约束 + 形态适配）

本仓库以 **「单技术点 Demo」** 为核心交付物，用本规范约束 AI 生成的每一个 demo；综合实战 Demo（如 `jdk8-seckill-demo` / `jdk8-short-link-demo`）仅保留、**不新增**，除非用户明确要求（见第七章）。

> 新生成的单技术点 demo，优先参考本仓库的 **`jdk8-localcache-demo`（Caffeine 本地缓存）** 的注释风格、测试形态与 README 七段式；但其**单模块目录结构只是形态之一**，当技术点天然多进程时，按本规范第二节选择多模块形态（见 Nacos / Seata 等）。

---

## 一、Demo 的定位

- **一个 Demo = 一个技术组件 / 一个技术点**（Redis、RocketMQ、MapStruct、本地缓存…）。
- 目标只有一条：**让看的人以最少噪音、最快速度抓住该技术点的关键机制**。
- 不是「能跑的小项目」，不是「微服务骨架」，不是「完整业务系统」。
- **形态因技术点而异**：技术点本身是单进程库，就用单模块；技术点本身是分布式/多进程基础设施（注册中心、配置中心、消息队列 broker、分布式事务 server…），就**拆成多个可独立启动的进程**来演示——后者不是特例豁免，而是还原该技术点的真实运行方式。

## 二、展示形态与目录结构（强制约束 + 形态适配）

**硬约束（与技术点形态无关，所有 demo 必须满足）：**
- **根包固定为 `lan.chaos.<tech>`**（如 `lan.chaos.redis`、`lan.chaos.localcache`）。**禁止** `lan.chaos.demo.<tech>` 这种带 `demo` 层的写法。
- 启动类命名 `<Tech>Application`（如 `LocalCacheApplication`、`NacosApplication`），放在该技术点根包下。
- 公共支撑统一收进 `common/`：`common/constant/`（topic / key / 缓存名 / 端口等常量，杜绝魔法值）、`common/config/`（Spring 配置类）、`common/model/`（样例数据模型）、`common/util/` 等；样例数据用 `sampleXxx()` 工厂。

**按技术点形态选择目录骨架：**

- **形态一 · 单模块（单进程库/组件）**：能力即顶层包，`lan.chaos.<tech>.<capability>`，每个 capability 聚焦一类知识点、一个场景一个类。
  - 正面：`cache/basic`、`cache/expire`、`lock`、`simple`、`order`、`basic`、`collection`…
  - `controller` / `service` / `repository` / `dao` / `dto` **不占顶层包**——它们只是触发外壳或支撑，收进 `common/` 或 `common/trigger/`（HTTP 端点等触发外壳仅当需 curl/Postman 交互式把玩时加，且不含知识点）。
- **形态二 · 多模块（天然多进程的基础设施）**：按「部署/角色单元」拆 Maven/Gradle 模块，顶层包名即部署单元名。
  - 例：Nacos 拆 `provider` / `consumer` / `config`，Seata 拆 `account` / `order` / `storage` + 独立 TC。
  - 此时 `controller` 是某部署单元对外暴露能力的入口，**允许占该模块的顶层**，因为多进程才是该技术点的真实形态；但**模块内部仍按能力分包**，公共支撑仍收进各模块的 `common/`。
  - 每个模块一个启动类（`<Role>Application`），整体共用根包 `lan.chaos.<tech>`。

## 三、场景代码规范（强制）

- **一个场景一个类**：类/方法单一职责，命名见名知意（`BasicCacheService`、`OrderProducer`、`DistributedLock`）。
- **必须自带样例数据**：提供 `sampleXxx()` 工厂造默认数据，调用方无需自己准备输入。
- **必须可观察**：场景执行后务必打印/返回「输入 → 输出」（日志、JSON、控制台），**静默执行是零分**。
- **注释讲 WHY 而非只写 WHAT**：每个能力类必须有一段说明——该机制解决什么痛点、关键 API 是什么、生产环境有什么坑/差异。
- **外部依赖最小化**：能用内存/JDK/Embedded 实现的（Caffeine、H2、EmbeddedKafka、MapStruct 编译期）就**不要**引真实中间件；确需外部组件时的处理顺序见 5.3。

## 四、验证与触发形态（强制，首选顺序）

1. **单元测试（首要形态，及格线）**：每个场景至少一条可断言的 `*Test`，既验证语义又充当「可观察输出」。无外部依赖时直接跑；CI 无外部依赖时靠 `Assumptions` 优雅跳过。**禁止**用「`@Disabled` 整体禁用 + `Thread.sleep` 当入口」的做法（那是 RocketMQ demo 的历史包袱，新 demo 不得复制）。
2. **控制台 Runner（可选）**：纯库/非 Web 想分节打印「输入→输出」时，提供 `DemoApp.main()`，用分隔线把每个场景的输出分节打印。
3. **场景注册表（可选，非强制）**：把场景抽象为 `Scenario { name(); description(); run(); }` 由 Spring 收集，经 `ApplicationRunner` 或测试加载，支持「按名 / 一键全跑」。

> 关键认知：**Demo 的好坏不靠 Web 控制器撑着**。没有 HTTP 端点，单元测试 + 控制台输出同样能把每个场景讲清楚、跑通、可观察。

## 五、标准化测试流程（强制）

本仓库以「单技术点 Demo」为核心交付物，其质量由测试保证。测试规范分两层：上层是第四节的**验证 / 触发形态**（讲「怎么演示、怎么触发」），本节是**工程 / 构建层**（讲「怎么算通过、怎么分层、用什么框架」），二者共同构成标准化测试流程。

### 5.1 测试分层与命名（金字塔）

| 层级 | 命名约定 | 执行生命周期 | 是否进 `mvn test` |
|------|----------|--------------|-------------------|
| 单元测试 | `*Test` / `*Tests` | `mvn test`（surefire） | 是（默认、最快） |
| 集成测试 | `*IT` | `mvn verify`（failsafe） | 否，仅 `verify` 阶段 |
| 压测 / 基准 | `*Bench` | 独立 `stress` profile | 否，避免污染单测、拖慢 CI |

- **默认只跑单测**：`mvn test` 必须快、稳定、零外部依赖或靠 `Assumptions` 跳过。
- **集成测试分流**：凡依赖真实 / 容器化中间件且无法内存化的用例，命名 `*IT`，由 failsafe 在 `verify` 阶段执行；禁止把集成测试伪造成 `*Test` 挤进单测阶段。
- **压测隔离**：所有 `*Bench` 必须排除在 surefire 之外，仅通过 `-Pstress` 单独跑；产物落 `bench-results/*.md`，**不得写入任何连接凭证 / 内网地址等敏感信息**（呼应根 `AGENTS.md` 公开项目脱敏约束）。
- **历史命名兼容**：既有 `BenchMarkTest`、`*IntegrationTest` 沿用旧名，由各平台父 pom 的 surefire 排除 / failsafe 包含规则直接兼容；**新增**测试请严格遵循 `*Bench` / `*IT` 命名。

### 5.2 框架统一（淘汰 JUnit4）

- 基线：**JUnit 5（Jupiter）+ AssertJ + Mockito**，全平台 JDK8+ 可跑。
- 大多数 Demo 直接引入 `spring-boot-starter-test`（已含上述三者）。
- 纯库 / 非 Spring Demo 允许裸 `org.junit.jupiter:junit-jupiter`，**不得退回 `junit:junit`（JUnit 4）**。
- 所有测试相关版本（junit-bom、assertj、mockito、testcontainers、surefire、failsafe、jacoco）**在平台级 parent `pom.xml` 的 `dependencyManagement` / `pluginManagement` 统一收口**，子模块不写版本号。

### 5.3 外部依赖处理矩阵（优先顺序）

1. **内存 / 内嵌**：能用 H2、EmbeddedKafka、内存 Redis、编译期 MapStruct 等就优先，零外部依赖、CI 自包含。
2. **Testcontainers**：需真实组件语义（如多数据源、事务）且本地有 Docker 时，用 Testcontainers 起临时容器，仅 `verify` 阶段跑。
3. **docker-compose + `Assumptions` 跳过**：demo 自带 `docker-compose.yml` 供交互式把玩；CI 无组件时 `Assumptions.assumeTrue(...)` 优雅跳过。
4. **敏感信息**：连接串 / 密码走 `application-local.yml`（已被 gitignore），测试读取本地 profile，绝不落库、不打印。

### 5.4 断言与可观察性

- 断言优先用 **AssertJ**（流式、可读、错误信息清晰），Mock 用 Mockito；**禁止用 `System.out` / `Thread.sleep` 充当测试断言**。
- 压测用例需输出可对比指标（吞吐 / P99 / 误差）并落报告。

### 5.5 覆盖率与质量门禁

- 各平台引入 **JaCoCo**，平台级设覆盖率阈值（如行覆盖 ≥ 60%，可按平台微调），未达标 `verify` 失败。
- 报告聚合到平台根 `target/site/jacoco/`，便于检视。

### 5.6 共享测试支撑模块

- 各平台维护一个 `*-common-test`（参考 `jdk8-microservice-demo/jdk8-ms-common-test`）：沉淀测试基类、`@Container` Testcontainers 助手、H2 / 嵌入式中间件约定、样例数据工厂 `sampleXxx()`。
- 该模块为 `test` scope 依赖，仅被其他模块的测试代码引用，不进运行时。

### 5.7 CI 串联（可选，推荐）

GitHub Actions（或等价 Runner）分阶段：
1. `mvn -q test` —— 单测 + 覆盖率门禁；
2. `mvn -q verify` —— 集成测试（`*IT`）；
3. `mvn -Pstress test` —— 压测 / 基准（按需、可手动触发）。

## 六、README 七段式（强制）

每个 A 类 Demo 的 README 必须包含：
1. **一句话定位**（学什么、覆盖哪些能力）
2. **技术栈与入口类、触发方式**
3. **快速开始**（docker-compose 如需 + 启动命令 + 验证命令/预期输出）
4. **场景一览**（按使用频率/难度排序，每条带触发方式 + 一句话解释）
5. **场景详解**（输入输出、关键 API、坑点）
6. **进阶方向**（生产化考量，即使未实现也列出，指明学习路径）
7. **设计要点**（为什么这么拆、频率结论）

完成后把所在平台 `README.md` 的「已完成学习记录」表对应状态更新为 ✅。

## 七、综合实战 Demo（可选，记录不强制）

- 综合实战 = 把多个技术点组合进一个带业务外壳的 demo（如 `jdk8-seckill-demo` / `jdk8-short-link-demo`）。
- 允许业务分层（controller/service/repository/dto/config），**不要求**对齐单模块 A 类的分包规则；但其中**每个被演示的技术点仍应满足本规范第三节的硬约束**（一个类讲清一个点、WHY 注释、可观察输出、可断言测试）。
- 在平台 `README.md` 中单独归类为「综合实战（锦上添花）」，标注「可选」。
- 不新增综合实战 demo，除非用户明确要求。

## 八、仓库结构约束（平台划分、模块命名、README 定位）

本章约束代码落在**哪个平台 / 以什么名字命名**，与第一~七章（Demo 内部怎么写）互补。

### 8.1 平台划分：JDK 约束取最小可用

- 技术对 JDK 有强约束（依赖库 / 字节码要求，如 `mybatis-plus-jsqlparser` 需 JDK 11+）→ 归入**能跑通该技术的最低版本平台**（`jdk11-platform` / `jdk17-platform` / `jdk21-platform` / `jdk25-platform`）。
- 技术对 JDK 无约束 → 一律归 `jdk8-platform`（默认位，兼容最广）。
- 已有技术点聚合组（`jdk8-tech` / `jdk21-tech` / `jdk8-office-tech`）能收纳的纯技术点示例，**先进对应聚合组**（子模块仍是单技术点 demo），不随意在平台根平铺新模块。

### 8.2 模块命名与目录形态

- 平台内模块统一 `jdk<version>-*-demo` 形式，**一个模块 = 一个技术点**。
- `jdk<version>-base`：对应 JDK 版本的基础知识与新特性。
- `jdk<version>-common`：平台内公共基础模块（占位 / 沉淀跨 demo 公共工具）。
- 聚合组模块：`jdk<version>-tech` 等父目录自含 `pom.xml`，组内每个子模块仍是单技术点 demo，须遵守本规范全部硬约束。
- 综合实战 demo（`jdk8-seckill-demo` / `jdk8-short-link-demo` / `jdk8-microservice-demo`）保留但**不新增**（见第七章）。
- 依赖 Demo 原样归位：同一技术点按 JDK 基线拆分时，旧基线模块若仍可跑**保留原位**，新拦截器 / 新特性版本放入更高版本平台，两者并存且各自说明差异。

### 8.3 版本与工程约定

- 依赖版本、测试框架版本与插件（surefire / failsafe / JaCoCo / `stress` profile）统一在平台根 `pom.xml` 的 `dependencyManagement` / `pluginManagement` 收口，子模块不写版本号。
- 引入新组件前先查平台根是否已管理该版本；确需覆盖时在子模块显式声明并注释原因。

### 8.4 README 定位

- 仓库根 `README.md` **只做模块树形概述**，不承载任何规则 / 约束 / 清单。
- 一切生成规范、仓库结构与自检约束写在本文件（`AGENTS.md`）。
- 平台 / 模块 `README.md` 的内容形态遵循第六章七段式。

---

## ⛳ 生成后自检（速查清单，细则见对应章节）

- [ ] 根包 `lan.chaos.<tech>`、无 `demo` 层；能力是顶层包，公共支撑进 `common/`（constant/config/model/util），`controller` 等不占顶层（形态二除外，见第二章）
- [ ] 多进程基础设施按部署 / 角色单元拆模块，每进程一个 `<Role>Application`，模块内仍按能力分包
- [ ] 一个场景一个类：`sampleXxx()` 样例数据 + WHY 注释 + 「输入→输出」可观察输出（第三章）
- [ ] 每场景至少一条可断言 `*Test`；无外部依赖直接跑、有依赖 `Assumptions` 跳过；禁 `@Disabled`+`sleep` 当入口（第四章）
- [ ] 测试分层 `*Test` / `*IT` / `*Bench`，JUnit5 + AssertJ + Mockito，版本在平台根 pom 收口（第五章）
- [ ] 外部依赖顺序 内存/Embedded > Testcontainers > docker-compose；敏感信息只走 `application-local.yml`（5.3）
- [ ] README 七段式；平台「已完成学习记录」置 ✅；根 `README.md` 树形概述同步（第六章 / 8.4）
- [ ] 归位：JDK 强约束 → 最低版本平台，无约束 → `jdk8-platform`，可入聚合组先入组（第八章）
- [ ] 模块名 `jdk<version>-<tech>-demo`；不新增综合实战 / 不随意建新聚合组（第七章 / 8.2）
