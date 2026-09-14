# jdk11-mybatis-plus-demo — MyBatis-Plus 高阶用法

> 本 demo 原位于 `jdk8-platform/jdk8-mybatis-plus-demo`。因依赖的 MyBatis-Plus 3.5.16 拦截器模块
> （`mybatis-plus-jsqlparser`，自 3.5.9+ 从 extension 拆分）需要 JDK 11+ 字节码，**无法在 JDK 8 上编译**，
> 故整体迁移到 `jdk11-platform`，按 JDK 11 构建运行。

## 一、一句话定位
基于 Spring Boot 2.7 + MyBatis-Plus 3.5.16，覆盖 6 个高频实战能力：条件构造器 Wrapper、分页（单表/联表）、
逻辑删除 + 乐观锁 + 自动填充、多租户隔离、动态表名分表、字段 AES 透明加密。每个能力一个类、自带样例数据、
可断言测试、零外部依赖（H2 内存库）。

## 二、技术栈与入口类、触发方式
- JDK：11（编译目标 `release=11`）
- Spring Boot：2.7.18
- MyBatis-Plus：`mybatis-plus-boot-starter` 3.5.16 + `mybatis-plus-jsqlparser` 3.5.16（分页/多租户等拦截器所在模块）
- 数据库：H2 内存库（内存即跑，无需 Docker）
- 入口类：`lan.chaos.mybatisplus.DemoApp`（`@SpringBootApplication`，同时是各 `*Test` 的 `@SpringBootTest` 配置源）
- 触发方式（按推荐顺序）：
  1. **单元测试（首选）**：`*Test` 覆盖每个场景，可断言、可观察。
  2. **控制台 Runner**：`mvn spring-boot:run`（或 `java -jar`）后由 `DemoApp` 分节打印「输入→输出」。

## 三、快速开始
```bash
# 在仓库根目录
mvn -pl jdk11-platform/jdk11-mybatis-plus-demo -am test

# 或仅启动看分节输出
cd jdk11-platform
mvn -pl jdk11-mybatis-plus-demo -am spring-boot:run
```
预期：`Tests run: 7, Failures: 0`；控制台打印 6 个场景的分节输出。

## 四、场景一览（按使用频率/难度排序）
| 场景 | 入口类 | 触发方式 | 一句话 |
| --- | --- | --- | --- |
| 条件构造器 | `wrapper.WrapperScenario` | `WrapperScenarioTest` | apply 安全拼接 + 自定义 SQL 复用 Wrapper 片段 |
| 分页 | `page.PageScenario` | `PageScenarioTest` | 单表 `selectPage` + 联表 `IPage<VO>` |
| 逻辑删除/乐观锁/填充 | `audit.AuditScenario` | `AuditScenarioTest` | 审计三件套一条龙 |
| 多租户隔离 | `tenant.TenantScenario` | `TenantScenarioTest` | 自动拼接 `tenant_id`，业务无感 |
| 动态表名分表 | `dynamictable.DynamicTableScenario` | `DynamicTableScenarioTest` | 按年份路由 `log_record_YYYY` |
| 字段 AES 加密 | `encrypt.EncryptScenario` | `EncryptScenarioTest` | 敏感字段落库密文、读取还原 |

## 五、场景详解
- **Wrapper**：`apply("age between {0} and {1}", 18, 40)` 用占位符参数绑定防注入；`${ew.customSqlSegment}` 把
  Wrapper 条件拼进手写 SQL（`UserMapper.selectByWrapper`）。`LambdaQueryWrapper` 在 VO 上无法建 lambda 缓存，故联表分页用普通 `QueryWrapper` + 列名，且用表别名 `o.create_time` 避免歧义列。
- **分页**：`PaginationInnerInterceptor` 拦截 `IPage` 参数，自动 `limit` + 额外 `count`。联表分页结果类型不限实体，`IPage<OrderUserVO>` 也行；排序列需用表别名限定。
- **审计三件套**：`@TableLogic` 把 DELETE 改写成 `SET deleted=1`；`@Version` 更新自动带 version 条件并自增；
  `MetaObjectHandler` 在 insert/update 写入 `create_time/update_time/operator`。
- **多租户**：`TenantLineInnerInterceptor` 仅对 `tenant_data` 注入 `tenant_id`（其余表 `ignoreTable` 返回 true 跳过），
  切换 `TenantContext` 即切换数据视野；INSERT 时也会自动写入当前租户值。
- **动态表名**：`DynamicTableNameInnerInterceptor` 按 `DynamicTableContext` 中的后缀把逻辑表 `log_record` 改写为
  `log_record_2024/_2025`，业务只操作逻辑表。
- **AES 加密**：`Member.phone` 标 `typeHandler=AesTypeHandler`，`autoResultMap=true` 保证「读取」也走 TypeHandler 解密；
  密钥硬编码仅为演示，生产用 KMS。

## 六、进阶方向
- 多租户：全局忽略表配置、跨租户 admin 操作、租户列统一治理。
- 动态表名：跨年/跨片查询的 union 聚合、历史数据归档。
- 分页：深翻页改用「游标分页」；联表分页 count 去重。
- 加密：改用 AES/GCM、密钥托管 KMS、为按手机号查询额外存确定性哈希列。
- 生产骨架：多数据源、读写分离、MP 代码生成器（`mybatis-plus-generator`）。

## 七、设计要点
- **为什么按能力拆包**：单模块形态下，一个能力一个包（`wrapper/page/audit/tenant/dynamictable/encrypt`），
  每个类单一职责、命名见名知意，配合 `common/{config,context,enums,handler,mapper,model,util}` 收公共支撑，
  杜绝魔法值、便于「最少噪音看懂一个知识点」。
- **为什么放在 jdk11 而非 jdk8**：MyBatis-Plus 自 3.5.9 起把分页/多租户等拦截器从 `mybatis-plus-extension`
  迁移到独立模块 `mybatis-plus-jsqlparser`，该模块字节码要求 JDK 11+，无法在 JDK 8 上编译运行，故本 demo 落在 jdk11-platform。
- **演示逻辑放 main 而非 CommandLineRunner**：`@SpringBootTest` 加载上下文时会自动执行所有 `CommandLineRunner` bean，
  若某场景抛错会导致「全部测试」上下文初始化失败；放 `main()` 里让单测干净加载上下文、直接调用各 Scenario 方法断言。

## 依赖要点
- `mybatis-plus-boot-starter` 3.5.16：覆盖父 POM 管理的 3.5.5。
- `mybatis-plus-jsqlparser` 3.5.16：提供 `PaginationInnerInterceptor` / `TenantLineInnerInterceptor` /
  `DynamicTableNameInnerInterceptor` / `OptimisticLockerInnerInterceptor`（3.5.9+ 起所在模块），其传递依赖的 `com.github.jsqlparser` 即为运行时所需解析器，无需额外声明。
- H2：`runtime` 即可；演示用 `MODE=MySQL`（支持反引号、LIKE 大小写不敏感）。
