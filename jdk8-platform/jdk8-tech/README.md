# jdk8-tech · 技术点示例组（JDK8 专属）

> 定位：把通用工程实践抽象为**纯技术点**示例，仅用于演示与学习。代码不含任何业务信息。
> **本组只放 JDK8 专属模块**——当前唯一成员是 Flink CDC（Flink 1.17 不兼容 JDK21，故不能进 jdk21-tech）。
> 其余技术点（多级缓存 / 虚拟线程 / 幂等 / 调优 / 工作流 / 安全）放 `jdk21-platform/jdk21-tech`。

## 已完成清单

| 模块 | 技术点 | 状态 | 基线 |
|------|--------|------|------|
| `jdk8-flink-cdc-sync-demo` | Flink CDC 同源库表同步 | ✅ | JDK8 |

## 运行方式
```bash
# 编译本组全部
mvn -q -pl jdk8-tech -am compile
# 跑单元/集成测试（无 MySQL 时集成测试自动跳过）
mvn -q -pl jdk8-tech/jdk8-flink-cdc-sync-demo test
```
