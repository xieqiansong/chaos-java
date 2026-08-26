# Flink CDC 同源库表同步 Demo

基于 Flink CDC（ververica `flink-connector-mysql-cdc`）实现的 MySQL 增量数据同步示例，
演示将一个 MySQL 实例中的源表实时同步到同一实例内的目标表（表名加 `_new` 后缀）。

本模块运行在 **JDK 8** 环境（整个 `jdk8-platform` 下仅此模块为 JDK 8 特例，其余模块均为 JDK 21）。

## 功能

- **MySQL Binlog 实时捕获**：通过 `MySqlSource` 读取源库 binlog，捕获 `INSERT / UPDATE / DELETE` 变更。
- **全量 + 增量**：使用 `StartupOptions.initial()`，作业启动时先扫描源表全量数据，再无缝衔接增量 binlog，不丢历史数据。
- **断点续传**：开启 Flink Checkpoint（默认 3 秒一次，落本地文件系统），作业异常重启后从上次位点继续消费，不重复、不遗漏。
- **通用字段映射**：通过 `IMapping` 接口描述「源表 → 目标表」的列映射关系，支持字段改名（如 `nickname → display_name`）、字段派生（如 `state=2` 时 `finished=1`）。新增同步表只需实现一个 `IMapping`。
- **目标表写入**：使用 Flink JDBC Sink，基于主键 `ON DUPLICATE KEY UPDATE` 实现幂等 upsert，删除事件对应 `DELETE`。

## 内置示例

| 源表 | 目标表 | 说明 |
|------|--------|------|
| `user` | `user_new` | 演示字段改名：`nickname → display_name`、`create_time → created_at` |
| `order` | `order_new` | 演示字段派生：`state` 推导 `finished`（`state=2` 视为已完成） |

## 目录结构

```
src/main/java/lan/chaos/flink/cdc/sync/
├── MysqlSyncJob.java                   # 作业入口
├── MySQLCDCSourceBuilder.java          # MySqlSource 构建
├── CommonTableDeserializationSchema.java  # SourceRecord → 统一 JSON
├── CheckpointConfig.java               # Checkpoint 配置
├── ConfigurationManager.java           # 配置读取
├── ValueHelper.java                    # 时间字段时区处理
├── DbSourceUtil.java                   # JDBC Sink 连接
└── mapping/                            # 表映射定义（IMapping 实现）
    ├── IMapping.java                    # 映射抽象接口
    ├── FieldMapping.java                # 字段映射
    ├── UserMapping.java                 # user → user_new
    └── OrderMapping.java                # order → order_new
src/main/resources/
├── application.properties              # 连接与同步配置
└── schema.sql                          # 建表 + 样例数据
```

## 快速开始

1. 准备 MySQL（开启 binlog，格式 `ROW`），建表并写入样例数据：
   ```bash
   mysql -h127.0.0.1 -P30100 -uroot -p<密码> demo < src/main/resources/schema.sql
   ```
2. 修改 `src/main/resources/application.properties` 中的连接信息（地址、端口、账号、密码、时区）。
3. 运行 `MysqlSyncJob.main()`，或打包后提交到 Flink：
   ```bash
   mvn -o package
   flink run target/jdk8-flink-cdc-sync-demo-1.0.0.jar
   ```
4. 启动后 `user_new` / `order_new` 立即被灌入初始数据；此后修改源表，目标表实时同步。

## 配置说明（application.properties）

| key | 说明 |
|-----|------|
| `source.server.address` / `port` | 源库（binlog 读取端）地址与端口 |
| `source.server.database` | 源库库名 |
| `source.server.timezone` | 源库时区（Flink CDC 要求时区名，如 `UTC` / `Asia/Shanghai`，须与数据库实际时区一致） |
| `source.server.server-id` | CDC 客户端 server-id，集群内须唯一 |
| `sink.jdbc.url` / `username` / `password` | 目标库 JDBC 连接信息 |
| `flink.ck-path` | Checkpoint 存储路径 |

## 技术要点

- **Binlog 不停摆**：Checkpoint + `server-id` 保证作业重启后可从 binlog 位点续传。
- **元数据驱动**：新增同步表只需增加一个 `IMapping` 实现并注册到 `MysqlSyncJob.MAPPINGS`，无需改 source/sink 代码。
- **时区一致性**：`source.server.timezone` 必须与 MySQL 实际时区一致，否则 CDC 校验直接失败。
