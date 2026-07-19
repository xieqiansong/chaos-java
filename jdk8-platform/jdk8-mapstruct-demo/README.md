# jdk8-mapstruct-demo

MapStruct 常见用法速查 Demo（学习向）。按**能力/特性**分类组织包，而非企业项目的 `entity/dto/mapper` 分层。
分类方式参考官方示例仓库 [`mapstruct-examples`](https://github.com/mapstruct/mapstruct-examples)（如 `mapstruct-clone`、`mapstruct-nested-bean-mappings`、`mapstruct-iterable-to-non-iterable`、`mapstruct-rounding` 等）。

## 目录结构

```
src/main/java/lan/chaos/mapstruct/
├── model/                  # 共享领域模型（被 basic / collection / custom 复用）
│   ├── User / Address / Gender / Role
├── basic/                  # 基础映射：clone / toDto / fromDto（含嵌套 Address）
│   ├── BasicMapper / UserDto / AddressDto
├── collection/             # 集合映射：List / Set
│   └── CollectionMapper     （复用 basic.UserDto）
├── nested/                 # 嵌套对象映射（自包含示例，对齐官方 nested-bean-mappings）
│   └── Person / Address / PersonDto / AddressDto / NestedMapper
├── custom/                 # 自定义映射：expression / constant / defaultValue / dateFormat / qualifiedByName
│   └── CustomMapper / UserSummaryDto / UserCardDto / MappingUtil
└── DemoApp.java            # 运行入口，打印各分类用法结果

src/test/java/lan/chaos/mapstruct/
├── basic/BasicMapperTest
├── collection/CollectionMapperTest
├── nested/NestedMapperTest
└── custom/CustomMapperTest
```

## 用法清单（按分类）

| 分类包 | 演示内容 | 关键注解 |
|---|---|---|
| `basic` | 同类型深拷贝 | `clone` |
| `basic` | 基础同名映射（含嵌套 Address） | `toDto` |
| `basic` | 反向映射 | `@InheritInverseConfiguration` |
| `collection` | 集合（List / Set）映射 | 复用单元素映射方法 |
| `nested` | 嵌套对象递归映射 | 字段同名自动递归 |
| `custom` | 字段名不一致 | `@Mapping(source=, target=)` |
| `custom` | 忽略字段 | `@Mapping(target=, ignore=true)` |
| `custom` | 常量填充 | `@Mapping(target=, constant=)` |
| `custom` | 默认值填充 | `@Mapping(source=, target=, defaultValue=)` |
| `custom` | 表达式填充 | `@Mapping(target=, expression="java(...)")` |
| `custom` | 日期格式化 | `@Mapping(dateFormat="...")` |
| `custom` | 自定义类型转换 | `@Mapper(uses=MappingUtil.class)` + `qualifiedByName` |

## 运行

编译与测试：

```bash
mvn -pl jdk8-platform/jdk8-mapstruct-demo test
```

运行演示入口（打印各映射结果）：

```bash
mvn -pl jdk8-platform/jdk8-mapstruct-demo exec:java -Dexec.mainClass=lan.chaos.mapstruct.DemoApp
```

也可直接在 IDE 中运行 `DemoApp.main` 或各 `*Test` 类。

## 补充说明

- **实例获取方式**：本 Demo 使用 `Mappers.getMapper(...)` + `INSTANCE` 常量，无需 Spring 容器即可运行。
- **与 Spring 集成**：在真实项目中通常改为 `@Mapper(componentModel = "spring")`，随后用 `@Autowired` 注入 Mapper 即可（需引入 Spring 依赖，本 Demo 未引入，故以 `INSTANCE` 演示）。
- **编译期生成**：MapStruct 在编译期根据注解生成实现类（`BasicMapperImpl` 等），可通过 `target/generated-sources` 查看生成代码。

## 依赖要点

- `mapstruct` + `mapstruct-processor`（注解处理器）
- `lombok` + `lombok-mapstruct-binding`（桥接器，确保 Lombok 与 MapStruct 协同）
- `hutool-all`（仅 `DemoApp` 打印输出使用）
- `junit-jupiter`（测试）
