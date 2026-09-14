# jdk8-localcache-demo  ★ A 类标杆模板

本地缓存（Caffeine）演示模块，覆盖**基础读写、写入过期(TTL)、容量淘汰(W-TinyLFU)、声明式缓存(@Cacheable)** 四个核心能力。**纯内存、零外部依赖**：无需 Redis / 数据库 / 任何中间件，跑测试或 main 即可看效果。

- 基础包：`lan.chaos.localcache`
- 技术栈：Spring Boot 2.7.18 + Caffeine 2.9.3（JDK8 用 2.x；JDK11+ 才用 3.x）
- 验证入口：`src/test/.../LocalCacheScenarioTest`（4 个场景均可断言，无外部依赖，任何环境直接跑）
- **本 demo 是后续所有 A 类 demo 的模板**：目录结构、注释风格、测试形态、README 七段式均照此（详见仓库根 `AGENTS.md`）

> 使用频率标注：`★★★ 高频`／`★★☆ 中频`／`◆ 基础`（连接/配置模块，非独立业务场景）。

## 目录结构

```
jdk8-localcache-demo/
├── pom.xml                                  # 继承 jdk8-platform，spring-boot-starter + starter-cache + caffeine
├── src/main/resources/application.yml       # 仅应用名与日志级别，无外部连接 ◆
    └── src/main/java/lan/chaos/localcache
    ├── LocalCacheApplication.java           # 启动类
    ├── DemoApp.java                         # 控制台 Runner：分节打印各场景「输入→输出」
    ├── common/                              # 支撑（配置/常量/模型，边缘关注，非场景）
    │   ├── config/
    │   │   └── CacheConfig.java             # @Cacheable 用的 CacheManager ◆
    │   ├── constant/
    │   │   └── CacheNameConstants.java      # 缓存名常量，避免魔法值
    │   └── model/
    │       └── User.java                    # 演示实体（自带 sample() 工厂）
    ├── basic/                               # 基础读写 ★★★
    │   └── BasicCacheService.java
    ├── expire/                              # 写入过期(TTL) ★★★
    │   └── ExpireCacheService.java
    ├── eviction/                            # 容量淘汰(W-TinyLFU) ★★★
    │   └── EvictionCacheService.java
    └── cacheaside/                          # 声明式缓存(@Cacheable) ★★☆
        └── CacheAsideService.java
```

> 设计要点：**能力场景是顶层包**（`basic/expire/eviction/cacheaside`），`config/constant/model` 这类「配置与支撑」统一收进 `common/`。这正是对后续 A 类 demo 的强制要求。

## 场景一览（按使用频率排序）

`★★★ 高频`
- [基础读写 basic](#1-基础读写-basic) → put / getIfPresent / invalidate
- [写入过期 expire](#2-写入过期-expire) → expireAfterWrite 实现最终一致
- [容量淘汰 eviction](#3-容量淘汰-eviction) → maximumSize + W-TinyLFU + stats 命中率

`★★☆ 中频`
- [声明式缓存 cacheaside](#4-声明式缓存-cacheaside) → @Cacheable 声明式缓存方法结果

`◆ 基础模块`
- [CacheConfig 缓存管理器](#cacheconfig-缓存管理器)

---

### CacheConfig 缓存管理器 `◆`

仅为 `cacheaside` 场景提供 `CacheManager`（CaffeineCacheManager，maximumSize=100、expireAfterWrite=5min）。`basic/expire/eviction` 三个场景各自用 Caffeine 原生 API 构建缓存，互不干扰，便于单独理解每个机制。

---

### 1. 基础读写 basic `★★★`

最常用结构：把热点对象放进堆内缓存，读时直接从内存取，跳过 DB/Redis。

- 写：`put(id, user)`；读：`getIfPresent(id)`（不触发加载）；失效：`invalidate(id)`
- 每个 `User` 由 `User.sample(id)` 工厂造出，无需自己准备数据

验证：见 `LocalCacheScenarioTest.basic_putThenGet_thenInvalidate`（写入可取、失效后为 null）。

---

### 2. 写入过期 expire `★★★`

本地缓存最怕「脏数据一直不更新」。`expireAfterWrite` 让写入后经过固定时间自动失效，下次读取拿不到（或走加载逻辑拿新值），实现最终一致。

- 配置：`expireAfterWrite(2, SECONDS)`（演示短 TTL；生产通常分钟~小时级）
- 也有 `expireAfterAccess`（空闲过期）、`expireAfter`（自定义读写后过期，最灵活）

验证：见 `LocalCacheScenarioTest.expire_valueGoneAfterTtl`（2s 后取值为 null）。

---

### 3. 容量淘汰 eviction `★★★`

堆内缓存必须限制大小，否则无限增长 OOM。Caffeine 默认用 **W-TinyLFU** 淘汰策略，比传统 LRU 命中率更高（能识别「短暂突发」与「真正热点」）。

- 配置：`maximumSize(3)` + `recordStats()`
- 观察：`cache.asMap().keySet()`（存活 key ≤ 上限）、`cache.stats().hitRate()`（命中率）

验证：见 `LocalCacheScenarioTest.eviction_keepsWithinMaximumSize`（写入 5 个、存活 ≤3）。

---

### 4. 声明式缓存 cacheaside `★★☆`

生产里绝大多数缓存只是「把方法返回值缓存起来」。用 `@Cacheable` 声明后，Spring 在方法调用前查缓存、命中直接返回，未命中才执行方法并把结果写回（Cache-Aside 模式）。

- 声明：`@Cacheable(cacheNames = COMPUTE, key = "#id")`，缓存名取自 `common/constant`
- 本场景 `load(id)` 内部 `dbCalls++` 模拟查库；同一 id 调两次，第二次应从缓存返回，`dbCalls == 1`

验证：见 `LocalCacheScenarioTest.cacheAside_secondCallHitsCache`。

---

## 如何运行

```bash
# 1) 跑测试（核心验证，纯内存零外部依赖，任何环境直接过）
mvn -pl jdk8-localcache-demo -am test

# 2) 或看控制台「输入→输出」：直接运行 DemoApp.main
mvn -pl jdk8-localcache-demo -am spring-boot:run   # 用 DemoApp 作入口时需把 mainClass 指向 DemoApp
# 更简单：在 IDE 里直接执行 DemoApp.main
```

预期（控制台节选）：

```
========== 1. 基础读写 basic ==========
put(1)         -> User(id=1, name=user-1, age=21)
getIfPresent(1) -> User(id=1, name=user-1, age=21)  (命中)
invalidate(1)   -> null  (已失效)

========== 4. 声明式缓存 @Cacheable ==========
第 1 次 load(1) -> User(id=1, name=user-1, age=21)
第 2 次 load(1) -> User(id=1, name=user-1, age=21)  (同一对象，来自缓存)
实际查库次数 dbCalls=1  (2 次调用只查了 1 次)
```

## 进阶方向（生产考量，未写成独立 Demo）

- `◆` **多级缓存**：本地(Caffeine) + 分布式(Redis) 组合，本地挡热点、Redis 挡共享，解决单机内存上限与集群一致性
- `◆` **缓存穿透/击穿/雪崩**：空值缓存/布隆过滤器、互斥锁/逻辑过期、随机 TTL 抖动
- `◆` **刷新与写策略**：`refreshAfterWrite` 后台刷新、Cache-Aside 的「写时失效」、Read-Through/Write-Through
- `◆` **监控**：Caffeine `stats()` 接入 Micrometer/Prometheus，观察命中率与淘汰量
- `◆` **Guava Cache 对比**：Caffeine 是 Guava Cache 的继任者，API 近似但命中率与性能更优

## 设计要点

- **零外部依赖**：纯内存演示，开箱即跑，是「最快反应当前技术点」的极致形态。
- **能力即顶层包**：`basic/expire/eviction/cacheaside` 各自聚焦一个机制，一个类讲清一个知识点。
- **手动 API 与声明式并存**：前三个场景用 Caffeine 原生 API（看清机制），第四个用 `@Cacheable`（看清生产常用形态），由 `common/config` 的 `CacheManager` 桥接。
- **频率结论**：基础读写 + 过期 + 容量淘汰是本地缓存的「三件套」，几乎必会；`@Cacheable` 声明式是生产最常用的接入方式。
