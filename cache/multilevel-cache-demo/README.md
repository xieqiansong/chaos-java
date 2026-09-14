# M1 多级缓存（Caffeine + Redis Hash + 版本号一致性）

> 多级缓存技术 Demo：L1=Caffeine 本地缓存，L2=Redis Hash + 业务域 VERSION 版本号，
> 读路径通过版本号比对跳过网络 IO，实现进程内缓存与分布式缓存的一致性。

## 设计要点

1. **两级结构**
   - **L1 Caffeine**：进程内本地缓存，`maximumSize` 容量上限 + `expireAfterWrite` TTL，
     避免朴素 `ConcurrentHashMap` 无界增长导致 OOM。
   - **L2 Redis Hash**：`DEMO:{bizKey}:{key}` 存字段级 Hash；`DEMO:{bizKey}:VERSION`
     存整个业务域的版本号。

2. **版本号一致性**
   - 任一节点 `put` 时 L2 版本号自增（`INCR`）。
   - 读路径先查 L1；L1 命中后**比对本地记录的版本号与 L2 当前版本号**：
     - 一致 → 直接返回本地副本，**跳过网络 IO 与反序列化**（高频读场景收益最大）。
     - 不一致 → 回源 L2 刷新 L1。
   - 删除/更新 → 版本号自增 → 其他节点 L1 自然失效，无需广播。

3. **并发安全**
   - 用 `ReadWriteLock` 保护「L1 读取 + 版本号判断 + L1 写入」临界区，避免竞态下
     L1 命中但版本号未刷新的脏读。

4. **可开箱即跑**
   - `multilevel-cache.redis-enabled=false` 时 L2 退化为内存实现（`InMemoryBackend`），
     无需本地 Redis 即可验证全部核心逻辑；置 `true` 即接真实 Redis。

## 运行

```bash
# 1) 单元测试（无需 Redis）
mvn -pl jdk21-tech/jdk21-multilevel-cache-demo -am test

# 2) 启动 Demo（默认内存版 L2，开箱即跑）
mvn -pl jdk21-tech/jdk21-multilevel-cache-demo spring-boot:run

# 3) 接真实 Redis：激活 local profile（application-local.yml 已设 redis-enabled: true，
#    并将端口/密码指向真实 Redis），PowerShell 下注意用 --% 透传 -D 参数
mvn --% -pl jdk21-tech/jdk21-multilevel-cache-demo spring-boot:run -Dspring.profiles.active=local
```

## 请求链路图

### 读路径（get）
```
        ┌─────────────── call get(key) ──────────────┐
        │                                            │
        ▼                                            │
 ┌─────────────┐   hit?                              │
 │  L1 Caffeine│── yes ──► compare version(local)    │
 └─────────────┘           vs version(L2 VERSION)    │
        │ no                     │                   │
        │                         ├── match ──► return L1 copy (skip network IO) ✅
        ▼                         └── mismatch ──► load from L2, refresh L1, return
 ┌─────────────┐                                      │
 │  L2 Backend  │── hash hit? ── yes ──► deserialize  │
 │ (Redis/Mem)  │                       → refresh L1 → return
 └─────────────┘                                      │
        │ no                                             │
        ▼                                               │
 ┌─────────────┐                                       │
 │ loadFromSource                                      │
 │ (in-mem / DB)  ──► put double-write(L2+L1) ─────────┘
 └─────────────┘
```
> 高频读下：L1 命中且版本号一致 → 零网络 IO 直接返回，这是 120ms→20ms 量级的收益来源。

### 写路径（put）
```
   put(key, value)
        │
        ├── 1. L2.putAll()  : 写 Redis Hash(DEMO:{bizKey}:{key}) + VERSION 自增(INCR)
        │
        └── 2. L1.put()     : 写 Caffeine + 更新 localVersionMap
```
> 双写顺序：先 L2(带版本号) 后 L1，保证其他节点能靠 VERSION 感知变更。

### 失效路径（remove）
```
   remove(key)
        │
        ├── 1. L2.remove()  : 删 Hash + VERSION 自增 → 其他节点 L1 下次读判为"不一致"
        │
        └── 2. L1.invalidate(): 清本节点 Caffeine + localVersionMap
```

## 数据源说明（是否用了数据库？）

**当前 Demo 没有接入任何数据库。** 回源（`loadFromSource`）由 `VehicleSource`
（一个内存 `ConcurrentHashMap` 充当的"模拟数据源"）提供，仅用于演示多级缓存的
读/写/失效机制本身，避免引入 MySQL/H2 等外部依赖导致无法开箱即跑。

真实场景里这里应替换为 `JdbcTemplate` / MyBatis / JPA 查询；若日后要补充"数据库调优"
相关模块，可在此处接入 H2 或本地 MySQL 并加 `EXPLAIN` 对照，与 M1 解耦独立成模块。

> 因此完整链路实际是：`L1(Caffeine) → L2(Redis Hash) → 内存数据源(模拟)`，
> 图中"真实 DB"为未来扩展位，当前不命中。

## 目录

```
jdk21-multilevel-cache-demo/
├── src/main/java/lan/chaos/multilevelcache/
│   ├── MultilevelCacheApplication.java   # 启动类
│   ├── DemoRunner.java                    # 演示读/写/失效全流程
│   ├── config/
│   │   ├── MultilevelCacheProperties.java # 配置项
│   │   └── CacheAutoConfiguration.java    # 按开关装配 L2 后端
│   ├── cache/
│   │   ├── CacheBackend.java              # L2 抽象（Redis / 内存）
│   │   ├── RedisHashBackend.java          # 生产形态 L2
│   │   ├── InMemoryBackend.java           # 退化形态 L2（无 Redis 可跑）
│   │   ├── AbstractMultilevelCacheable.java # 核心模板：Caffeine + 版本号
│   │   ├── MapRedisCacheable.java         # 车辆缓存具体实现
│   │   └── VehicleSource.java             # 演示用内存数据源
│   └── model/Vehicle.java                 # 车辆实体（演示用）
└── src/test/.../MapRedisCacheableTest.java
```
