# jdk8-excel-demo

Excel 导入导出 Demo：覆盖**原生 POI / EasyExcel / Hutool** 三大体系的写入、读取、大文件流式处理、模板填充，并用**相同数据**对多种工具做导入 / 导出压测横评。

## 技术栈

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 8 | |
| Spring Boot | 平台 BOM | 仅用于装配，非 Web 容器 |
| Apache POI | **5.5.1** | HSSF / XSSF / SXSSF，平台 BOM 收敛 |
| Alibaba EasyExcel | **4.0.3** | POI 之上的流式封装（跑在它没编译过的 POI 5.5.1 上，故有版本矩阵守门） |
| Hutool-all | 最新 | Excel 轻量封装（底层仍是 POI 全内存） |
| 测试 | JUnit 5 + Spring Boot Test | CI 直接跑，零外部依赖 |

## 快速开始

```bash
# 在模块目录执行；以下默认均在本目录内

mvn test                                            # 跑全部测试（含横评）
mvn exec:java -Dexec.mainClass=lan.chaos.excel.DemoApp   # 分节打印每个能力（0 依赖版本 ~ 8 导入横评）

# 只跑横评（默认 2 万行，可调更大规模）
mvn test -Dtest=ExcelBenchTest -Dbench.rows=20000
mvn test -Dtest=ExcelBenchTest -Dbench.rows=100000   # 看大文件下内存分野
```

## 能力清单（对应 DemoApp 1 ~ 8 节）

| 节 | 能力 | 要点 |
| --- | --- | --- |
| 1 | 基础 | HSSF(.xls，硬上限 **65536** 行) / XSSF(.xlsx 全内存) / SXSSF（滑动窗口流式，**刷出的行不可再改**）|
| 2 | 样式与公式 | `CellStyle` 必须复用（进程上限 **64000** 个）；公式必须先求值再读，否则读回 0 |
| 3 | 大文件读取 | 用户模型（全内存，OOM 头号元凶）vs **SAX 事件模型**（流式、内存恒定）|
| 4 | EasyExcel | 注解式写、`CSV`、监听器**分批读**（批量入库接入点）、模板填充 |
| 5 | Hutool 轻量封装（对照）| 三行写出；底层仍是 POI 全内存 |
| 6 | 功能场景测试 | 见「测试」一节 |
| 7 | **导出横评** | 同一份 2 万行 × 7 列，喂 6 个写方案对比 |
| 8 | **导入横评** | 读同一个文件，4 个读方案对比 |

## 压测横评（相同数据对比）

- **导出**：同一份数据喂 `XSSF / SXSSF(窗口100) / SXSSF(窗口1000) / EasyExcel / CSV / Hutool` 六个写方案，对比耗时 / 内存增量 / 文件大小。
- **导入**：读同一个 `big-orders.xlsx`，用 `用户模型 / SAX / EasyExcel 监听器(每批1000) / Hutool` 四个读方案，对比耗时 / 内存增量 / 读到行数（均 20000 行，确认非空跑）。

结论摘要：导出 **EasyExcel / CSV 最快最省**；**Hutool 全内存封装最慢最吃内存**（印证"底层仍是 POI 全内存"）；导入 **EasyExcel 监听器综合最优**（快且省）。完整数据见 [TEST-REPORT.md](./TEST-REPORT.md) 与 `target/bench-results.md`。

## 测试

3 个测试类、共 **16 个用例，全部通过**（CI 直接跑，纯本地、零外部依赖）：

| 测试类 | 用例数 | 类型 | 验证点 |
| --- | --- | --- | --- |
| `ExcelScenarioTest` | 11 | 功能场景 | 每个能力「写→读往返」强验证：三种 Workbook 往返行数一致、`.xls` 超 65536 必须失败、SXSSF 刷出行取不回、公式求值、两种读法行数一致、EasyExcel 分批读、模板填充、脏数据不中断导入逐条收集、Hutool 往返… |
| `VersionMatrixTest` | 4 | 版本守门 | POI 与 poi-ooxml 同版本 **5.5.1**（收敛掉 EasyExcel 传递的 5.2.5）；commons-compress **1.28.0** / commons-io **2.21.0** 不被 Spring Boot BOM 压回（防 CVE 修复被吞）；EasyExcel 在锁定 POI 上读写往返一致 |
| `ExcelBenchTest` | 1 | 压测横评 | 跑导出 + 导入横评并落报告 |

测试报告：[TEST-REPORT.md](./TEST-REPORT.md)（综合）/ `target/bench-results.md`（纯横评）。

## 关键技术坑（摘要，详见各 Service 类注释）

- **HSSF 上限 65536 行**；XSSF 内存随行数线性增长；SXSSF 刷出窗口的行不可再读、改样式会失败。
- 单元格样式对象有 **64000** 上限，必须复用，否则抛异常。
- 公式读取前必须求值，否则为 0。
- SAX 读取：共享字符串表整体进内存、需解析 `cellReference` 处理空单元格、只能顺序读。
- Hutool 不自带 POI；`@Alias` 与 `@ExcelProperty` 不互通；写已存在文件会脏数据（须先删）。
- EasyExcel 底层仍是 POI；监听器内不能逐条写库，须攒批（`PageReadListener`）。

## 产物

- 演示产物：`target/out/`
- 横评报告：`target/bench-results.md`
- 综合测试报告：[TEST-REPORT.md](./TEST-REPORT.md)
