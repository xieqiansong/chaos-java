# id-demo — 分布式 ID 生成方案

一句话定位：5 种分布式 ID 生成方案的对比小样：UUID / Snowflake(Hutool) / Redis-INCR / 美团 Leaf / 滴滴 TinyId。

## 场景一览

| 方案 | 代表类 | 外部依赖 |
|---|---|---|
| UUID | `distributed.system.distributed.id.UUIDExample` | 无 |
| Snowflake | `distributed.system.distributed.id.SnowflakeHutoolExample` | 无（Hutool 内置） |
| Redis INCR | `distributed.system.distributed.id.RedisIdExample` | Redis（默认 localhost:30102） |
| 美团 Leaf | `distributed.system.distributed.id.LeafSegmentExample` | Leaf 号段服务端（`SegmentService` 由 Spring 注入） |
| 滴滴 TinyId | `distributed.system.distributed.id.TinyIdExample` | TinyId 服务端 + 本地仓库已有 `tinyid-client` 快照 |

## 快速开始

均带 `main`，IDE 直接运行。未起对应外部服务的示例连接失败属正常。

## 设计要点

前两种零依赖直接跑；后三种需外部服务/客户端。本模块自原 `jdk8-base` 拆分而来，包名保持不变。
