# jdk21-tech · JDK21 技术点 Demo 聚合

> 聚合模块：每个子模块是单一技术点的可运行 Demo，根包统一 `lan.chaos.<tech>`，遵循 `chaos-java/AGENTS.md`。

## 模块一览

| 模块 | 技术点 | 状态 |
|------|--------|------|
| `jdk21-multilevel-cache-demo` | 多级缓存 Caffeine + Redis 版本号 | ✅ 已完成 |
| `jdk21-idempotent` | 接口幂等（请求/消费/状态机三层去重） | ✅ 已完成 |
| `jdk21-virtualthread-demo` | 虚拟线程（吞吐对比/载体调度观察/pinning 复现/结构化并发/ThreadLocal 语义） | ✅ 已完成 |
| `jdk21-db-tune` | 索引/慢SQL + 本地事务/最终一致 | ⏳ 待做 |
| `jdk21-workflow` | 最小工作流引擎 + 回调幂等 | ⏳ 待做 |
| `jdk21-security` | 安全合规基线（密码/锁定/审计） | ⏳ 待做 |

## 构建

```bash
# 全部模块编译测试
mvn -pl jdk21-tech -am test
```
