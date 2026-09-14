# jdk8-starter-demo

> 一句话定位：用一个轻量、零依赖的 `token-spring-boot-starter`，把 **Spring Boot Starter 自动装配机制**讲透——自动配置类、`@ConfigurationProperties` 外部化配置、`@ConditionalOn*` 条件装配、命名约定，以及「引依赖即用 / 可开关 / 可被覆盖」三大契约。功能（生成 token）只是载体，机制才是重点。

## 1. 技术栈与入口类、触发方式

- **技术栈**：JDK 8 + Spring Boot 2.7.18（2.7 起推荐 `AutoConfiguration.imports` 取代 `spring.factories`）。
- **根包**：`lan.chaos.starter`（遵循 AGENTS.md，无 `demo` 中间层）。
- **入口类 / 触发方式**：
  - 制造方：`lan.chaos.starter.autoconfigure.TokenAutoConfiguration`（自动配置类）。
  - 使用方：`lan.chaos.starter.usage.StarterUsageApplication`（Spring Boot 启动即装配 `TokenService`）。
  - 控制台：`lan.chaos.starter.usage.DemoApp.main()` 分节打印三个机制场景。
  - 验证（及格线）：`TokenAutoConfigurationTest`（4 条可断言单元测试，零外部依赖）。

## 2. 快速开始

```bash
# 位置：jdk8-platform/jdk8-starter-demo
mvn -pl jdk8-starter-demo -am test            # 跑单元测试（核心验证）
mvn -pl jdk8-starter-demo -am spring-boot:run # 启动使用方应用，控制台打印生成 token
mvn -pl jdk8-starter-demo -am exec:java -Dexec.mainClass=lan.chaos.starter.usage.DemoApp  # 控制台分节演示
```

预期输出（使用方启动）：
```
=== Starter 使用方启动验证 ===
生效配置: TokenProperties(enabled=true, length=32, prefix=, charset=SIMPLE)
生成 token: ...(32 位随机串)
```

## 3. 场景一览

| 场景 | 触发方式 | 一句话解释 |
|------|----------|------------|
| 默认自动装配 | `@Autowired TokenService` | 用户不写任何 `@Bean`，starter 默认提供 `TokenService` |
| 外部化配置 | `application.yml` 配 `token.starter.*` | `@ConfigurationProperties` 绑定前缀，改配置即改行为 |
| 可开关 | `token.starter.enabled=false` | `@ConditionalOnProperty` 关闭后容器不再有该 Bean |
| 可被覆盖 | 用户自定义 `TokenService` Bean | `@ConditionalOnMissingBean` 生效，starter 默认实现让位 |
| 命名约定 | 见 `StarterConstant` | 第三方 starter 命名 `xxx-spring-boot-starter`，与官方区分 |

## 4. 场景详解

### 4.1 自动装配（核心）
`TokenAutoConfiguration` 经 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 被 Spring Boot 在启动时主动加载（**非** `@ComponentScan` 扫到）。它 `@EnableConfigurationProperties(TokenProperties.class)` 注册配置绑定类，并 `@Bean` 提供 `TokenService`。

关键 API：`@Configuration` / `@EnableConfigurationProperties` / `@Bean`。
生产坑：自动配置类不要放在扫描包内，否则失去「按需装配」意义。

### 4.2 外部化配置
`TokenProperties` 用 `@ConfigurationProperties(prefix = "token.starter")` 把 `application.yml` 的配置项绑定到字段，配合 `spring-boot-configuration-processor` 为 IDE 生成补全元数据。开箱即用默认值（length=32, charset=SIMPLE），用户可覆盖。

### 4.3 条件装配（可开关 / 可被覆盖）
- `@ConditionalOnProperty(prefix="token.starter", name="enabled", havingValue="true", matchIfMissing=true)`：开关。
- `@ConditionalOnMissingBean`：仅在用户未自定义时提供默认 `TokenService`。

### 4.4 命名约定（细节坑）
- 官方：`spring-boot-starter-xxx`（如 `spring-boot-starter-web`）。
- 第三方：`xxx-spring-boot-starter`（如本 demo 的 `token-spring-boot-starter`、MyBatis-Plus 的 `mybatis-plus-spring-boot-starter`）。
- 配置前缀用业务名（如 `token.starter`），不要占用 `spring.*`。

## 5. 进阶方向（生产化考量）

- **不可变绑定**：生产用 `@ConstructorBinding` + `final` 字段，避免运行时被意外改。
- **装配顺序**：多自动配置有依赖时，用 `@AutoConfigureAfter` / `@AutoConfigureBefore` / `@AutoConfigureOrder` 控制。
- **元数据与提示**：在 `additional-spring-configuration-metadata.json` 为配置项补 `description`、默认值提示。
- **独立发布**：把 `autoconfigure` + `TokenService` 拆成独立 `token-spring-boot-starter` 模块（本 demo 为演示合并同模块），使用方仅引依赖。
- **条件细化**：`@ConditionalOnClass` 判断可选依赖是否存在（如仅当 Redis 在 classpath 才装配 Redis 实现）。

## 6. 设计要点

- **为什么拆制造方 / 使用方**：starter 的本质是「制造方提供约定，使用方零配置消费」。同模块内用 `autoconfigure` / `token` / `usage` 三层表达这一关系，避免拆多模块带来的噪音（机制是单进程库范式，走形态一）。
- **频率结论**：自动装配 > 外部化配置 > 条件装配 > 命名约定。前两者是「能用」，后两者是「好用且不被踩坑」，README 与注释均按此优先级讲解。

---

> 平台总表状态：`jdk8-platform/README.md` 对应条目已置 ✅。
