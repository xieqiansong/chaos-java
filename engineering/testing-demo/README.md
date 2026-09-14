# jdk8-testing-demo — 单元测试专项（Mockito + Spring Boot 切片测试）

## 一、一句话定位

学习 JUnit4 + Mockito 单元测试核心技巧，覆盖 **@Mock/@Spy/@InjectMocks、参数匹配、行为验证、BDD 风格**，以及 **Spring Boot @WebMvcTest 切片测试**。

## 二、技术栈与入口

- **测试框架**：JUnit 4 + Mockito（spring-boot-starter-test 内置）
- **切片测试**：Spring Boot Test（@WebMvcTest + MockMvc）
- **主入口**：`lan.chaos.testing.TestingApplication`
- **触发方式**：直接运行 `*Test` 类（无需启动应用，纯单元测试无外部依赖）

## 三、快速开始

```bash
# 编译并运行所有测试（无需启动外部服务）
cd jdk8-platform/jdk8-testing-demo
mvn test
```

## 四、场景一览

| # | 场景 | 测试类 | 一句话 |
|---|------|--------|--------|
| 1 | @Mock / @Spy / @InjectMocks | `MockitoBasicTest` | 三剑客用法对比，spy 用 doReturn 而非 when |
| 2 | 参数匹配器 | `MockitoArgumentTest` | argThat 自定义条件、any+eq 混用、正则匹配 |
| 3 | 行为验证 | `MockitoVerificationTest` | verify/times/never/atLeast/InOrder 全套验证 |
| 4 | BDD 风格 | `BDDMockitoTest` | given/when/then 可读测试，跨角色沟通 |
| 5 | @WebMvcTest 切片 | `SpringMvcSliceTest` | MockMvc 模拟 HTTP 请求，只加载 Controller 层 |

## 五、场景详解

### 场景 1：@Mock / @Spy / @InjectMocks

- **输入**：UserService 依赖 UserRepository（接口）
- **输出**：@Mock → 默认返回 null/empty；stub 后返回预设值；@Spy → 部分真实 + 部分 mock
- **关键 API**：`@RunWith(MockitoJUnitRunner.class)` 自动初始化注解
- **坑**：@Spy 下 stub 用 `doReturn().when()` 而非 `when().thenReturn()`（后者会调真实方法）

### 场景 2：参数匹配器

- **输入**：不同 User 实例（VIP vs 普通）
- **输出**：`argThat` 只匹配满足自定义条件的参数
- **关键 API**：`any()`, `eq()`, `argThat(condition)`, `matches(regex)`
- **坑**：一旦使用 any()，所有参数都必须用匹配器，具体值也要 eq() 包裹

### 场景 3：行为验证

- **输入**：多次调用 createUser
- **输出**：verify 断言 save 被调用 3 次、InOrder 验证调用顺序
- **关键 API**：`verify(mock, times(n))`, `never()`, `atLeast(n)`, `InOrder`
- **坑**：verify 默认检查 exactly 1 次

### 场景 4：BDD 风格

- **输入**：given...willReturn stub
- **输出**：then...should 验证
- **关键 API**：`BDDMockito.given()`, `BDDMockito.then()`
- **坑**：BDDMockito 只是 Mockito 的别名，功能完全一致

### 场景 5：@WebMvcTest 切片

- **输入**：MockMvc 模拟 GET/POST 请求
- **输出**：验证 HTTP 状态码、响应体、JSON 路径
- **关键 API**：`mockMvc.perform()` + `andExpect()` + `jsonPath()`

## 六、进阶方向

- [ ] JUnit 5 迁移（@ExtendWith + @Nested + @DisplayName）
- [ ] @DataJpaTest 数据层切片测试（H2 内存数据库）
- [ ] TestContainers 集成测试（真实 MySQL/Redis 容器）
- [ ] WireMock 模拟外部 HTTP API
- [ ] JaCoCo 测试覆盖率报告
- [ ] Mutation Testing（PITest）

## 七、设计要点

- **测试即 demo**：本 demo 以 test 目录为核心，`src/main` 仅提供 scaffold
- **无外部依赖**：所有 test 纯 Mockito mock，`@WebMvcTest` 仅加载 Controller 层
- **一个类一个知识点**：5 个测试类分别聚焦 Mock 基础、参数匹配、验证、BDD、切片
