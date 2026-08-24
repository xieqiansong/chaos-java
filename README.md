# chaos-java

> Java 多版本生态下的**中间件最小可用设计**学习聚合仓库。以顶层 `jdk<version>-platform` 划分工程，一个平台承载一批可独立运行的技术 Demo，按「**JDK 约束取最小可用**」归位。

## 设计理念

顶层仅按 JDK 版本划分出多个 `jdk<version>-platform` 聚合工程，核心原则是**中间件最小可用设计**：

- **技术对 JDK 有强制要求** → 归入对应的最低版本平台。
  例如 MyBatis-Plus 3.5.9 起的分页/多租户拦截器拆分到 `mybatis-plus-jsqlparser`，要求 JDK 11+，故对应 Demo 迁至 `jdk11-platform`。
- **技术对 JDK 无要求** → 全部归入 `jdk8-platform`（兼容性最广，作为默认位）。

这样每个平台都能用**刚够用的 JDK**跑通它承载的中间件，既不浪费、也不因版本不足而报错；平台之间通过 Maven 多模块聚合，版本、依赖统一由各平台 `pom.xml` 集中管理。

## 平台总览

| 平台 | JDK | Spring Boot | 承载内容 | 定位 |
|------|-----|-------------|----------|------|
| [jdk8-platform](file:///d:/project/chaos/chaos-java/jdk8-platform/README.md) | 8 | 2.7.18 | 22 个模块：Java 基础内功、Redis / 本地缓存 / MyBatis-Plus / Nacos / 微服务 / RocketMQ / Kafka / Sentinel / Seata / ES / ZK / Security / 加密 / 序列化 / 定时 / MapStruct / 秒杀 / 短链 / Starter / 测试 等 | 默认位，无 JDK 强约束的技术 |
| [jdk11-platform](file:///d:/project/chaos/chaos-java/jdk11-platform) | 11 | 2.7.18 | jdk11-base（新特性）、jdk11-common、jdk11-mybatis-plus-demo | 需 JDK 11+ 的技术（如 MyBatis-Plus 拦截器） |
| [jdk17-platform](file:///d:/project/chaos/chaos-java/jdk17-platform) | 17 | 3.5.14 | jdk17-base（新特性：密封类等）、jdk17-common | 需 JDK 17+ 的技术 |
| [jdk21-platform](file:///d:/project/chaos/chaos-java/jdk21-platform) | 21 | 3.5.14 | jdk21-base（新特性）、jdk21-common | 需 JDK 21+ 的技术 |
| [jdk25-platform](file:///d:/project/chaos/chaos-java/jdk25-platform) | 25 | 4.0.7 | jdk25-base（新特性）、jdk25-common | 需 JDK 25+ 的技术 |

> 各平台详情、模块清单与学习记录见对应平台 `README.md`。

## 模块命名约定

- 各平台内统一 `jdk<version>-*-demo` 形式（如 `jdk8-redis-demo`），一个模块 = 一个技术点。
- `jdk<version>-base`：对应 JDK 版本的基础知识与新特性。
- `jdk<version>-common`：平台内公共基础模块。
- Demo 生成与展示形态遵循根目录 [`AGENTS.md`](file:///d:/project/chaos/chaos-java/AGENTS.md) 的硬约束（单技术点、一个类讲清一个点、样例数据、可断言测试、WHY 注释、外部依赖最小化）。

## 快速开始

每个平台是独立的多模块 Maven 工程，可单独构建：

```bash
# 进入某个平台（示例：JDK8 平台）
cd jdk8-platform
# 编译并跑一个 demo 的测试
mvn -pl jdk8-redis-demo test
```

多数 Demo 自带单元测试（无外部依赖时直接跑），需外部组件的（Redis / Kafka / Nacos 等）附 `docker-compose.yml`，按对应 README「先起组件再起 Demo」执行。

## 相关文档

- [AGENTS.md](file:///d:/project/chaos/chaos-java/AGENTS.md) — Demo 生成规范（硬约束 + 形态适配 + 自检清单）
- [jdk8-platform](file:///d:/project/chaos/chaos-java/jdk8-platform/README.md) — 主平台模块总览、技术栈、补充计划
- [jdk8-platform/JAVA-TECH-PANORAMA.md](file:///d:/project/chaos/chaos-java/jdk8-platform/JAVA-TECH-PANORAMA.md) — Java 必学技术全景与学习路线