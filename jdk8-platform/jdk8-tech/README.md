# jdk8-tech · 技术点示例组（JDK8 专属）

> 定位：把通用工程实践抽象为**纯技术点**示例，仅用于演示与学习。代码不含任何业务信息。
> **本组放 JDK8 专属模块**：Flink CDC（Flink 1.17 不兼容 JDK21，故不能进 jdk21-tech）；
> 以及源码基线为 JDK8 的纯技术点示例（如 HMAC 无状态签名鉴权，纯 JDK8 即可落地，无需进 jdk21-tech）。
> 其余技术点（多级缓存 / 虚拟线程 / 幂等 / 调优 / 工作流 / 安全）放 `jdk21-platform/jdk21-tech`。

## 已完成清单

| 模块 | 技术点 | 状态 | 基线 |
|------|--------|------|------|
| `jdk8-flink-cdc-sync-demo` | Flink CDC 同源库表同步 | ✅ | JDK8 |
| `jdk8-hmac-auth-demo` | HMAC 无状态签名鉴权（签名/防重放/轮换/吞吐对比） | ✅ | JDK8 |
| `jdk8-bitmap-stat-demo` | Bitmap 统计（在线状态/日活 BITOP/内存账/吞吐/大 key 拆分优化） | ✅ | JDK8 |

## 运行方式
```bash
# 编译本组全部
mvn -q -pl jdk8-tech -am compile
# 跑单元/集成测试（无 MySQL 时集成测试自动跳过）
mvn -q -pl jdk8-tech/jdk8-flink-cdc-sync-demo test
# HMAC 鉴权 Demo：测试 + 四场景演示
mvn -q -pl jdk8-tech/jdk8-hmac-auth-demo test
java -cp jdk8-tech/jdk8-hmac-auth-demo/target/classes lan.chaos.hmac.HmacAuthDemo
# Bitmap 统计 Demo：测试 + 五场景演示
mvn -q -pl jdk8-tech/jdk8-bitmap-stat-demo test
java -cp jdk8-tech/jdk8-bitmap-stat-demo/target/classes lan.chaos.bitmap.BitmapStatDemo
```
