# jdk8-webflux-demo — WebFlux 响应式编程

WebFlux 响应式编程演示模块，覆盖 **Reactor 响应式类型与操作符（Mono/Flux）、背压（Backpressure）、RouterFunction 函数式路由、注解式响应式 Controller（对照）、WebClient 异步非阻塞调用** 五个核心能力。基于 **Spring Boot 2.7.18 + WebFlux（Netty 非阻塞）**，JDK 8 下 WebFlux 需 Spring Boot 2.x（本项目即用 2.7.18）。**纯内存、零外部中间件**：仓储用内存 Map，WebClient 调本应用内嵌端点，跑测试或 main 即可看效果。

- 基础包：`lan.chaos.webflux`
- 技术栈：Spring Boot 2.7.18 + spring-boot-starter-webflux（底层 Netty）+ reactor-test（测试）
- 验证入口：`src/test/...` 下 5 个 `*Test`（Reactor 用 StepVerifier，端点用 WebTestClient），无外部依赖，任何环境直接跑
- **本 demo 是「WebFlux 响应式」专项模板**，遵循仓库根 `AGENTS.md` 的「AI 生成自检清单」

> 使用频率标注：`★★★ 高频`／`★★☆ 中频`／`◆ 基础`（连接/配置模块，非独立业务场景）。

## 目录结构

```
jdk8-webflux-demo/
├── pom.xml                                  # 继承 jdk8-platform；spring-boot-starter-webflux + reactor-test
├── src/main/resources/application.yml       # 固定端口 18080（demo 用；测试用 RANDOM_PORT 覆盖）
└── src/main/java/lan/chaos/webflux/
    ├── WebFluxApplication.java              # 启动类
    ├── DemoApp.java                         # 控制台 Runner：分节打印各场景「输入→输出」
    ├── common/                              # 支撑（配置/常量/模型/仓储，边缘关注，非场景）
    │   ├── config/WebClientConfig.java       # WebClient.Builder（统一 responseTimeout）◆
    │   ├── constant/ApiConstants.java        # 路由/路径常量，避免魔法值 ◆
    │   ├── model/Product.java               # 样例实体（自带 sample() 工厂）◆
    │   └── repository/InMemoryProductRepository.java  # 内存仓储（支撑）◆
    ├── reactor/                             # Reactor 响应式类型与操作符 ★★★
    │   ├── MonoFluxBasics.java               # Mono/Flux + map/flatMap/filter
    │   └── BackpressureDemo.java            # 背压：request(n) 按需拉取 + 策略
    ├── router/                              # RouterFunction 函数式路由 ★★☆
    │   ├── ProductRouter.java               # 路由声明（GET/POST）
    │   └── ProductHandler.java              # HandlerFunction 处理
    ├── controller/                          # 注解式响应式 Controller（对照）★★☆
    │   └── ProductController.java           # @RestController 返回 Mono/Flux
    └── webclient/                           # WebClient 异步非阻塞调用 ★★☆
        └── ProductWebClient.java            # 声明式 HTTP 客户端
```

> 设计要点：**能力场景是顶层包**（`reactor/router/controller/webclient`），`config/constant/model/repository` 这类「配置与支撑」统一收进 `common/`。

## 场景一览（按使用频率排序）

`★★★ 高频`
- [Reactor 基础 reactor/MonoFluxBasics](#1-reactor-响应式类型与操作符-monofluxbasics) → Mono/Flux + map/flatMap/filter
- [背压 reactor/BackpressureDemo](#2-背压-backpressuredemo) → request(n) 按需拉取 + 策略

`★★☆ 中频`
- [RouterFunction 函数式路由 router](#3-routerfunction-函数式路由) → 替代 @Controller 的函数式端点
- [注解式响应式 Controller controller](#4-注解式响应式-controller-对照) → 从 MVC 平滑迁移的形态
- [WebClient 异步调用 webclient](#5-webclient-异步非阻塞调用) → 非阻塞 HTTP 客户端

`◆ 基础模块`
- [WebClientConfig 客户端配置](#webclientconfig-客户端配置)

---

### WebClientConfig 客户端配置 `◆`

仅为 WebClient 提供统一 `WebClient.Builder` 并设 `responseTimeout=3s`。「非阻塞」不等于「无限等待」——下游长时间不返回会一直占用连接，超时后 Mono 以错误终结，调用方用 `onErrorResume` 兜底。

---

### 1. Reactor 响应式类型与操作符 MonoFluxBasics `★★★`

WebFlux 的引擎。Mono = 0|1 个元素，Flux = 0..N 个元素，二者都是「声明式数据流」——直到订阅才执行，且天然非阻塞、内建背压。

- 创建：`Flux.fromIterable` / `Mono.justOrEmpty`
- 同步转换：`map`（如 `price*1.1`）
- 异步展开：`flatMap`（如 `delayElement` 模拟远程调用后再合并）
- 过滤：`filter(stock>0)`
- 汇聚/终结：`collectList()` / `block()`（`block` 仅测试/演示，生产禁止）

验证：见 `MonoFluxBasicsTest`（StepVerifier 断言每个操作符语义）。

---

### 2. 背压 BackpressureDemo `★★★`

响应式区别于「回调 / 线程池」的核心。生产者可能远快于消费者，没背压会撑爆消费者内存。Reactor 通过 `request(n)` 让消费者「按需拉取」，而不是生产者硬推。

- 拉不过来时的策略：`onBackpressureBuffer`（缓存等待）/ `onBackpressureDrop`（直接丢弃）/ `onBackpressureLatest`（只留最新）
- 演示：用 `BaseSubscriber` 钩子，订阅时 `request(5)`，处理完再要下一批

验证：见 `BackpressureTest.fastProducer_respectsManualRequest`——`thenRequest(5)` 后只收到 1..5，`thenRequest(15)` 后才收 6..20，证明背压生效。

---

### 3. RouterFunction 函数式路由 `★★☆`

WebFlux 对 @Controller 注解的替代。路由与处理都是「函数」：`route().GET(...).POST(...)` 链式声明，类型安全、可组合、易测试，不走反射/注解扫描。适合「端点少、要极致可控 / 可拼装」的场景。

- 声明：`ProductRouter.productRoutes()` 把 `/api/products` 的 GET/POST 绑到 `ProductHandler`
- 处理：`ProductHandler` 每个方法收 `ServerRequest`、返回 `Mono<ServerResponse>`（非阻塞）

验证：见 `ProductRouterTest`（WebTestClient 直连内嵌端点，覆盖 list/getById/notFound/create）。

---

### 4. 注解式响应式 Controller（对照） `★★☆`

`@RestController` + Mono/Flux 返回值，是「从 Spring MVC 平滑迁移」的形态——写法与 MVC 几乎一致，只是方法返回 Mono/Flux 即非阻塞。适合端点多、团队熟悉 MVC 的项目。与 RouterFunction 二选一；两者底层都是同一套 WebFlux 引擎，路径不冲突即可共存（此处用 `/api/annotated` 前缀区分）。

验证：见 `ProductControllerTest`（与函数式路由对照，证明两种写法都可用）。

---

### 5. WebClient 异步非阻塞调用 `★★☆`

WebFlux 的声明式 HTTP 客户端。相比 RestTemplate（每请求占一个线程），WebClient 全程非阻塞（底层 Netty），高并发下省大量线程。返回 Mono/Flux，可链式 `flatMap`/`zip` 编排多次调用。

- `fetchProduct(id)`：`GET /api/products/{id}` → `bodyToMono(Product.class)`
- `fetchAll()`：`GET /api/products` → `bodyToFlux`
- `createAndFetch(product)`：`POST` 后返回创建的实体
- baseUrl 端口取自运行端口（`local.server.port`，测试随机；demo 固定 18080）

验证：见 `ProductWebClientTest`（调本应用内嵌端点，非阻塞可用）。

---

## 如何运行

```bash
# 1) 跑测试（核心验证，纯内存零外部依赖，任何环境直接过）
mvn -pl jdk8-webflux-demo -am test

# 2) 看控制台「输入→输出」：直接运行 DemoApp.main
mvn -pl jdk8-webflux-demo -am spring-boot:run   # 用 WebFluxApplication 作入口
# 更简单：在 IDE 里直接执行 DemoApp.main（会拉起 Netty，再调 WebClient 场景）

# 3) 交互式把玩端点（需先启动应用，默认 18080）
curl http://localhost:18080/api/products
curl http://localhost:18080/api/products/1
curl -X POST http://localhost:18080/api/products -H 'Content-Type: application/json' \
     -d '{"id":100,"name":"product-100","price":1000,"stock":500}'
curl http://localhost:18080/api/annotated/products   # 注解式对照
```

预期（DemoApp 控制台节选）：

```
========== 1. Reactor 响应式类型与操作符 ==========
fluxFromList()            -> 5 个商品
map(price*1.1) 首件      -> Product(id=1, name=product-1, price=11.0, stock=5)
flatMap 异步补全库存 末件 -> Product(id=5, name=product-5, price=50.0, stock=105)
filter(stock>0) 命中数    -> 5
monoFromId(3)             -> Product(id=3, name=product-3, price=30.0, stock=15)

========== 2. 背压 Backpressure ==========
onSubscribe: 首次 request(5)
  拉到 1 (累计 1)
  ...（每次只拉 5 个，处理完再要下一批）...
onComplete: 消费者共处理 20 个

========== 3. WebClient 异步非阻塞调用 ==========
GET /api/products/{id} -> Product(id=1, name=product-1, price=10.0, stock=5)
GET /api/products -> 共 5 个
POST /api/products -> Product(id=99, name=product-99, price=990.0, stock=495)
```

## 进阶方向（生产考量，未写成独立 Demo）

- `◆` **SSE / WebSocket 流式推送**：`MediaType.TEXT_EVENT_STREAM` + `Flux` 做服务端推送（股票行情、日志流）；WebSocketHandler 做双向流
- `◆` **响应式持久层**：R2DBC（非阻塞关系库）/ Reactive MongoDB / Reactive Redis，整条链路不阻塞线程
- `◆` **背压落地策略**：`onBackpressureBuffer(max, DROP_OLDEST)` 在「拉不过来」时按业务取舍（缓存 vs 丢旧 vs 丢新）
- `◆` **错误处理与超时**：`onErrorResume` / `timeout` / `retryWhen`（指数退避），配合 WebClient 的 `responseTimeout`
- `◆` **与 MVC 选型**：IO 密集、高并发、需要流式/背压 → WebFlux；简单 CRUD、团队熟 MVC、要同步事务 → MVC（或虚拟线程），不必强行响应式

## 设计要点

- **零外部依赖**：仓储内存 Map、WebClient 调本应用内嵌端点，开箱即跑，是「最快反应当前技术点」的极致形态。
- **能力即顶层包**：`reactor/router/controller/webclient` 各自聚焦一个机制，一个类讲清一个知识点。
- **两种端点写法并存对照**：`RouterFunction`（函数式、类型安全）与 `@RestController`（从 MVC 平滑迁移）二选一，由 `common/constant` 路径前缀隔离，便于理解「同一引擎、两种形态」。
- **测试分层清晰**：Reactor 语义用 `StepVerifier`（纯库、不需上下文）；端点用 `WebTestClient` 直连内嵌 Netty（RANDOM_PORT、零外部依赖），均进 `mvn test`。
- **频率结论**：Reactor 类型与背压是地基（必会），函数式路由 / 注解 Controller / WebClient 是上层三种最常用的「接入形态」。
