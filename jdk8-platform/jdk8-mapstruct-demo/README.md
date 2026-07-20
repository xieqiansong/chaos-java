# jdk8-mapstruct-demo

## 一、一句话定位
MapStruct 常见用法速查 Demo（学习向）。按**能力/特性**分包，覆盖「基础克隆/同名映射/反向映射、集合映射、嵌套递归、自定义映射（重命名/忽略/常量/默认值/表达式/日期格式化/枚举转义）」，**纯编译期、零外部依赖**，每个场景均有可断言单元测试 + 控制台分节打印。

分类方式参考官方示例仓库 [`mapstruct-examples`](https://github.com/mapstruct/mapstruct-examples)（如 `mapstruct-clone`、`mapstruct-nested-bean-mappings`、`mapstruct-iterable-to-non-iterable`、`mapstruct-rounding` 等）。

## 二、技术栈与入口类、触发方式
- **技术栈**：MapStruct 1.5.5.Final + Lombok 1.18.30 + `lombok-mapstruct-binding` 桥接 + hutool（仅打印）+ JUnit 5。
- **根包**：`lan.chaos.mapstruct`（无 `demo` 中间层）。
- **入口类**：
  - 控制台演示：`lan.chaos.mapstruct.DemoApp#main`（按 basic/collection/nested/custom 分节打印各映射结果）。
  - 验证主形态（首选）：各 `*Test`（见下）。
- **触发方式**：`mvn test` 直接跑断言；或 `mvn exec:java` 跑 `DemoApp` 看控制台输出。

## 三、快速开始
```bash
# 编译 + 全部单元测试（首选验证手段，12 条断言全过）
mvn -pl jdk8-platform/jdk8-mapstruct-demo test

# 控制台分节打印各场景「输入 → 输出」（可选）
mvn -pl jdk8-platform/jdk8-mapstruct-demo exec:java -Dexec.mainClass=lan.chaos.mapstruct.DemoApp
```
- **测试预期输出**：`Tests run: 12, Failures: 0, Errors: 0`（basic 4 / collection 3 / custom 3 / nested 2）。
- **控制台预期输出**：按 `=== basic ===` / `=== collection ===` / `=== nested ===` / `=== custom ===` 分节，打印 `User` 与 `UserDto`/`UserSummaryDto`/`UserCardDto` 之间的映射前后 JSON。

## 四、场景一览（按常用度排序）
| 场景 | 触发方式 | 一句话说明 |
|---|---|---|
| 基础同名映射（含嵌套 Address） | `BasicMapperTest#toDto_mapsNested` | 字段同名自动映射，嵌套对象递归映射 |
| 同类型深拷贝 | `BasicMapperTest#clone_copiesAllFields` | `clone` 方法做对象深拷贝 |
| 反向映射 | `BasicMapperTest#toEntity_inheritsInverse` | `@InheritInverseConfiguration` 复用正向配置 |
| 集合映射（List / Set） | `CollectionMapperTest` | 复用单元素映射方法做批量映射 |
| 嵌套递归 | `NestedMapperTest` | 字段同名自动递归，无需额外注解 |
| 字段重命名 / 忽略 | `CustomMapperTest#toSummary_*` | `@Mapping(source=, target=)` / `ignore=true` |
| 常量 / 默认值 | `CustomMapperTest#toSummary_*` | `constant=` 填常量；`defaultValue=` 仅源为 null 生效 |
| 表达式拼接 | `CustomMapperTest#toSummary_mapsRenameIgnoreConstantExpression` | `expression="java(...)"` 手写任意 Java 逻辑 |
| 日期格式化 | `CustomMapperTest#toCard_mapsQualifiedDateFormatAndNested` | `dateFormat="yyyy-MM-dd"` 格式化 LocalDate/Time |
| 枚举转义（自定义转换） | `CustomMapperTest#toCard_mapsQualifiedDateFormatAndNested` | `@Mapper(uses=MappingUtil)` + `@Named` + `qualifiedByName` |

## 五、场景详解（关键 API 与坑点）
| 能力 | 关键注解 / API | 生产坑点 |
|---|---|---|
| 基础映射 | `@Mapper` + 同名字段自动映射 | 字段名/类型不一致**不会**自动映射，需 `@Mapping` 显式声明 |
| 反向映射 | `@InheritInverseConfiguration` | 仅继承字段映射配置，表达式/常量需自行对齐 |
| 集合映射 | 定义单元素映射，List/Set 自动复用 | 集合元素类型须与单元素映射入参/出参一致 |
| 嵌套 | 字段同名 | 嵌套层级深时仍自动递归，无需逐层声明 |
| 重命名 | `@Mapping(source="username", target="account")` | 多个源字段映射到同一目标会编译报错 |
| 忽略 | `@Mapping(target="phone", ignore=true)` | 目标字段无源对应时**不写会报错**，必须显式 ignore |
| 常量 | `@Mapping(target="type", constant="SUMMARY")` | constant 与 source 互斥，不能同时写 |
| 默认值 | `@Mapping(source="level", target="grade", defaultValue="NORMAL")` | **仅在源字段为 null 时生效**；空串 `""`、0 不触发 |
| 表达式 | `@Mapping(target="displayName", expression="java(realName + '(' + username + ')')")` | 表达式内用 Java 全限定名/import，可读性差，慎用 |
| 日期格式化 | `@Mapping(dateFormat="yyyy-MM-dd")` | 源必须是 `Date`/`LocalDate`/`LocalDateTime`，否则转换失败 |
| 枚举转义 | `@Mapper(uses=MappingUtil.class)` + `@Named("genderToDesc")` + `qualifiedByName` | `MappingUtil` 方法须 `@Named` 标注，否则 `qualifiedByName` 找不到 |

## 六、进阶方向（生产化考量，本项目未实现）
- **与 Spring 集成**：真实项目通常 `@Mapper(componentModel = "spring")`，后用 `@Autowired` 注入（本 Demo 用 `Mappers.getMapper(...)` + `INSTANCE`，免容器）。
- **查看生成代码**：MapStruct 在**编译期**生成实现类（`BasicMapperImpl` 等），位于 `target/generated-sources`，可对照理解其映射逻辑。
- **Lombok 桥接**：必须引入 `lombok-mapstruct-binding`，否则 `@Data` 的 getter 在 MapStruct 处理时尚未生成，导致映射失败。
- **选型对比**：与 `BeanUtils.copyProperties`、ModelMapper 相比，MapStruct 是编译期生成、零运行时反射、性能最佳，但需写 Mapper 接口；DTO 频繁变动时维护成本略高。
- **拓展场景**：批量 DTO 组装、流式映射、`@AfterMapping`/`@BeforeMapping` 后置处理、多源对象合并映射。

## 七、设计要点
- **为什么按能力分包而非 `entity/dto/mapper` 分层**：单技术点 Demo 的目标是「最快反应当前技术点的关键机制」，按能力（`basic/collection/nested/custom`）组织能让读者一眼定位某个注解的用法，而企业分层会把同一个技术点打散到多个层。
- **共享模型收 `common/model`**：`User`/`Address`/`Gender`/`Role` 是被多个能力包复用的领域模型，统一放在 `common/model`，避免让 `model/` 占顶层包（与 AGENTS 规范一致）。
- **测试优先**：每个能力包一个 `*Test`，既验证语义又充当「可观察输出」；自包含、零外部依赖，`mvn test` 即看结果。
- **样例数据自包含**：`DemoApp#sampleUser()` 造默认数据，测试与演示统一复用，调用方无需自行准备输入。
