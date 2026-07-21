# jdk8-mybatis-plus-demo

MyBatis-Plus 高阶用法速查 Demo（学习向）。覆盖日常项目里最容易「只会 CRUD、不懂原理」的几块：
**条件构造器、分页、逻辑删除 + 乐观锁 + 自动填充、多租户隔离、动态表名分表、字段透明加密**。
每个能力一个独立、可触发、可观察的场景（`*Scenario`），既有单元测试断言语义，也能用 `DemoApp.main` 一键打印「输入 → 输出」。

> 技术栈：JDK 8 + Spring Boot 2.7.18 + MyBatis-Plus **3.5.3.2**（刻意锁定该版本：3.5.7+ 起移除了
> `PaginationInnerInterceptor` / `TenantLineInnerInterceptor` / `BlockAttackInnerInterceptor` 等内置拦截器，
> 3.5.3.2 仍内置，便于在一处演示完整插件链）+ H2 内存库（零外部依赖，CI 自包含）。

## 目录结构

```
src/main/java/lan/chaos/mybatisplus/
├── DemoApp.java                  # 运行入口：main() 启动后逐场景打印结果（非 @Bean CommandLineRunner）
├── common/
│   ├── constant/                 # 公共常量（租户键、动态表名上下文键等）
│   ├── context/                  # TenantContext / DynamicTableContext（ThreadLocal 传参）
│   ├── handler/AesTypeHandler    # 字段级 AES 加解密 TypeHandler
│   ├── util/AesUtil              # AES 工具（与 AesTypeHandler 共用，保证可反向解密校验）
│   ├── config/                   # Spring 配置类（统一收进 common）
│   │   ├── MybatisPlusConfig         # 插件链装配：分页 / 多租户 / 动态表名 / 防全表更新
│   │   ├── MyTenantLineHandler       # 多租户：哪些表忽略、租户 ID 从哪来
│   │   ├── MyDynamicTableNameHandler # 动态表名：log_record → log_record_${year}
│   │   └── SchemaInit                # 上下文启动时执行 schema.sql + data.sql 初始化 H2
│   └── enums/UserStatusEnum      # 用户状态枚举（@EnumValue 标记存库值）
├── wrapper/   WrapperScenario   # ① 条件构造器：LambdaQueryWrapper + 自定义 SQL 拼接 Wrapper
├── page/     PageScenario       # ② 分页：单表 selectPage + 联表自定义 SQL 分页
├── audit/    AuditScenario      # ③ 逻辑删除 + 乐观锁 + 自动填充
├── tenant/   TenantScenario     # ④ 多租户隔离
├── dynamictable/ DynamicTableScenario # ⑤ 动态表名分表
├── encrypt/  EncryptScenario    # ⑥ 字段 AES 透明加密
├── entity/  User / Member / Order / TenantData / LogRecord / OrderUserVO
└── mapper/  UserMapper / MemberMapper / OrderMapper / LogRecordMapper
```

## 场景一览（按使用频率/难度排序）

| 场景 | 触发方式 | 一句话解释 |
|------|----------|------------|
| ① 条件构造器 `wrapper` | `WrapperScenario.complexQuery()` / `customSqlWithWrapper()` | `LambdaQueryWrapper` 拼 `between/like/orderBy`；自定义 `@Select` + `${ew.customSqlSegment}` 复用 Wrapper |
| ② 分页 `page` | `PageScenario.userPage()` / `orderUserPage()` | `selectPage` 单表分页；联表自定义 SQL + `IPage`，MP 自动补 `count` |
| ③ 审计 `audit` | `AuditScenario.logicDeleteAndVersion()` | 逻辑删除（WHERE deleted=0）+ 乐观锁（version 自增）+ 自动填充审计字段 |
| ④ 多租户 `tenant` | `TenantScenario.tenantIsolation()` | `TenantLineInnerInterceptor` 透明注入 `tenant_id` 条件，跨租户互不可见 |
| ⑤ 动态表名 `dynamictable` | `DynamicTableScenario.routeByYear()` | `DynamicTableNameInnerInterceptor` 按年份把 `log_record` 路由到 `log_record_2024/2025` |
| ⑥ 字段加密 `encrypt` | `EncryptScenario.transparentEncrypt()` | `AesTypeHandler` 让手机号落库即密文、读取自动还原，业务无感 |

## 快速开始

无需任何外部组件（H2 内存库 + 内存模式），克隆即跑：

```bash
# 运行全部单元测试（每个场景一条可断言的 *Test，CI 无外部依赖）
mvn -pl jdk8-platform/jdk8-mybatis-plus-demo test

# 运行控制台入口，逐场景打印「输入 → 输出」
mvn -pl jdk8-platform/jdk8-mybatis-plus-demo spring-boot:run
# 或打包后 java -jar：
mvn -pl jdk8-platform/jdk8-mybatis-plus-demo package && java -jar jdk8-platform/jdk8-mybatis-plus-demo/target/*.jar
```

`spring-boot:run` 预期输出（节选）：

```
===== MyBatis-Plus 高阶用法 Demo =====

[1] Wrapper 高阶条件构造
  complexQuery 命中 2 条: [...]
  customSqlWithWrapper 命中 N 条: [...]

[6] 字段 AES 透明加密
  {dbIsCipher=true, decryptEqual=true, cipherDecryptOk=true}
```

`dbIsCipher=true` 表示落库确实是密文；`decryptEqual=true` 表示读取自动还原成明文；`cipherDecryptOk=true` 表示用 `AesUtil.decrypt` 能反向解出原值。

## 场景详解（坑点与设计取舍）

### ① 条件构造器 `WrapperScenario`
- `complexQuery`：`LambdaQueryWrapper<User>` 拼 `between(age,18,40)` + `like(name,"a")` + `orderByAsc(id)`，
  演示「不写 SQL 也能拼出安全参数化查询」。
- `customSqlWithWrapper`：`UserMapper` 上 `@Select("SELECT * FROM t_user ${ew.customSqlSegment}")`，
  把 Wrapper 的 WHERE/ORDER BY 拼进手写 SQL——既保留手写 SQL 的掌控力，又复用 Wrapper 的条件能力。
- **坑**：`user` / `order` 都是 SQL 保留字，表名加 `t_` 前缀（`t_user` / `t_order`），否则 `FROM user` 直接语法错。

### ② 分页 `PageScenario`
- 单表：`userMapper.selectPage(new Page<>(current,size), null)`，MP 自动拦截并改写 `LIMIT`。
- 联表：`OrderMapper.selectOrderUserPage(IPage, Wrapper)`，自定义 SQL 里写 `JOIN` 与 `COUNT`，
  MP 用 `IPage` 入参自动识别「这是分页请求」并补 `count(*)`。
- **坑**：联表排序别写不存在的列。`OrderUserVO` 是结果 VO 不是实体，用 `QueryWrapper` + 列名字符串
  （`orderByDesc("o.create_time")`）而非 `LambdaQueryWrapper`（VO 无 lambda 缓存）；且 `o.create_time`
  要用表别名限定，否则 `t_order` 与 `t_user` 都有 `create_time` 会报 `Ambiguous column`。

### ③ 逻辑删除 + 乐观锁 + 自动填充 `AuditScenario`
- 逻辑删除：`@TableLogic` 字段 `deleted`，MP 自动把 `DELETE` 转 `UPDATE ... SET deleted=1`，查询自动加 `deleted=0`。
- 乐观锁：`@Version` 字段 `version`，更新时 `SET version=version+1 WHERE version=?`，并发改同一条会 `updatedRows=0`。
- 自动填充：`MetaHandler` 实现 `MetaObjectHandler`，在 `INSERT/UPDATE` 时填充 `createTime/updateTime/operator`。
- **坑**：`version` 列默认给 `1`（插入后 version=1，首次更新自增为 2），别给 `0` 否则与「首次更新后为 1」的直觉不符。

### ④ 多租户隔离 `TenantScenario`
- `TenantLineInnerInterceptor` + `MyTenantLineHandler`：对**非忽略表**透明追加 `tenant_id = ?` 条件。
- 租户 ID 从 `TenantContext`（ThreadLocal）取，模拟「当前登录租户」；`tenant_data` 表本身忽略租户条件。
- 体会 at-least-once 式的「无感注入」——业务代码完全不写 `WHERE tenant_id`。

### ⑤ 动态表名分表 `DynamicTableScenario`
- `DynamicTableNameInnerInterceptor` + `MyDynamicTableNameHandler`：把逻辑表名 `log_record` 按 `DynamicTableContext`
  里的年份路由到物理表 `log_record_2024` / `log_record_2025`（典型按年分表）。
- 切换分表只需 `DynamicTableContext.setSuffix("2025")`，SQL 里的表名随之变化。

### ⑥ 字段透明加密 `EncryptScenario`
- `Member.phone` 标 `@TableField(typeHandler = AesTypeHandler.class)`：写入加密、读取解密，业务无感。
- **关键坑（极易踩）**：
  - **只读不写加密**：`@TableField(typeHandler=...)` 默认只在「写入参数」时生效；要让 `selectById` 读取也能解密，
    必须给实体加 `@TableName(value="member", autoResultMap = true)`，MP 才会生成含 typeHandler 的结果映射。
  - **别用 `type-handlers-package` 全局扫描注册** `AesTypeHandler`，那会把它注册成「全局 String 处理器」，
    导致**所有 VARCHAR 字段（如 name）被误加密**。本 Demo 只用 `@TableField` 精确标注，不影响其它列。
  - 加密/解密共用 `AesUtil`，保证 `AesUtil.decrypt(密文)` 能还原，便于单元测试做反向校验。

## 进阶方向（生产化考量，即使未实现也值得了解）

- **多租户**：生产 tenant_id 多来自登录态 / JWT，配合 Redis 缓存租户路由；避免把租户字段写进忽略表。
- **动态表名**：真实分表常配合「路由维度」+ 历史归档（冷热分离）；注意跨表查询需业务层聚合。
- **字段加密**：本 Demo 用固定密钥仅作演示；生产应密钥托管（KMS）、字段级密钥、或国密 SM4；超长字段注意 `VARCHAR` 容量。
- **乐观锁**：仅防「并发改同一条」，不替代分布式事务；失败需业务重试或冲突提示。
- **逻辑删除**：注意唯一索引与逻辑删除共存时的冲突（MP 提供 `@TableLogic` + 逻辑删除字段组合唯一键方案）。

## 依赖要点

- `spring-boot-starter` + `mybatis-plus-boot-starter`（版本 3.5.3.2，由父 POM `dependencyManagement` 统一管理）
- `jsqlparser:4.6`：**显式声明**。MP 3.5.3.2 的分页/多租户拦截器运行时依赖它，但 MP 未传递；
  注意 4.7+ 已移除 `net.sf.jsqlparser.statement.select.SelectExpressionItem`，与 3.5.3.2 不兼容，故锁 4.6。
- `h2`（runtime）：内存库，零外部依赖。
- `spring-boot-starter-test` + `junit-jupiter`：以单元测试为核心验证手段。
