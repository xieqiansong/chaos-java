# jdk8-excel-demo 测试报告

- **生成时间**：2026-09-04
- **模块**：`jdk8-excel-demo`（JDK 8 Office 技术 Demo）
- **运行方式**：`mvn test`（横评默认 2 万行；可调 `-Dbench.rows=100000`）
- **结论**：✅ 全部通过，0 失败 / 0 错误 / 0 跳过

---

## 一、总览

| 测试类 | 类型 | 用例数 | 失败 | 错误 | 跳过 | 耗时 |
| --- | --- | --- | --- | --- | --- | --- |
| `ExcelScenarioTest` | 功能场景（写→读往返） | 11 | 0 | 0 | 0 | 2.19 s |
| `VersionMatrixTest` | 依赖版本守门 | 4 | 0 | 0 | 0 | 0.06 s |
| `ExcelBenchTest` | 压测横评（导出+导入） | 1 | 0 | 0 | 0 | 27.82 s |
| **合计** | — | **16** | **0** | **0** | **0** | **≈ 30 s** |

> 横评（`ExcelBenchTest`）占绝大部分耗时，因为它要真实写出/读回 2 万行 × 7 列 × 10 个方案；功能与版本测试都是毫秒级，纯本地、零外部依赖，CI 直接跑。

---

## 二、测试套件明细

### 1. `ExcelScenarioTest` —— 每个能力一条可断言的"写→读往返"
验证重点是"能用"而非"好看"：写出去的文件必须能被读回来，且行数/数值一致。

| 用例 | 验证点 |
| --- | --- |
| `basic_threeWorkbookTypesWriteAndReadBack` | HSSF(.xls) / XSSF(.xlsx) / SXSSF 三种写法往返行数一致 |
| `basic_hssfRejectsMoreThan65536Rows` | .xls 超 65536 行必须**明确失败**，而非写出打不开的文件 |
| `basic_sxssfFlushedRowCannotBeReadBack` | SXSSF 滑出窗口的行取不回（导出中途改样式的坑） |
| `write_formulaMustBeEvaluatedBeforeReading` | 公式必须先求值再读，否则读回 0 |
| `read_userModelAndSaxMustAgreeOnRowCount` | 用户模型读法与 SAX 读法行数一致 |
| `easyexcel_writeThenReadByBatches` | EasyExcel 写→分批监听读（每批 100，300 行触发 3 批） |
| `easyexcel_templateFillProducesHeaderPlusDataRows` | 模板填充 = 1 表头 + 50 数据行 |
| `easyexcel_csvAndXlsxBothProduced` | 同数据 xlsx 与 csv 均成功产出 |
| `importCheck_dirtyRowsAreCollectedInsteadOfAborting` | 脏数据不中断导入，逐条收集（100 正常 + 4 脏） |
| `importCheck_errorReportCanBeExported` | 错误明细可导出成 xlsx 供下载 |
| `hutool_writeAndReadBack` | Hutool 写 100 行原样读回 100 行 |

### 2. `VersionMatrixTest` —— 守住"版本错配"这一最大雷区
Office 专题最易翻车的是 EasyExcel 跑在它没编译过的 POI 版本上，症状是运行期 `NoSuchMethodError`/`ClassNotFoundException`，堆栈落在 POI 内部。

| 用例 | 验证点 |
| --- | --- |
| `printVersionMatrix` | 打印运行期实际版本；断言 `poi-ooxml-lite` 在场、EasyExcel = 4.0.3 |
| `poiArtifactsMustShareSameVersion` | poi 与 poi-ooxml 同版本（锁定 **5.5.1**，收敛掉 EasyExcel 传递的 5.2.5） |
| `securityCriticalDepsMustNotBeDowngradedBySpringBootBom` | commons-compress = **1.28.0**、commons-io = **2.21.0**（防 Spring Boot BOM 悄悄压回老版本吃掉 CVE 修复） |
| `easyExcelRoundTripOnLockedPoiVersion` | EasyExcel 在锁定 POI 5.5.1 上读写往返一致（金额 BigDecimal 精确） |

### 3. `ExcelBenchTest` —— 同数据 × 不同工具横评
跑导出/导入两组横评，结果同时落到 `target/bench-results.md` 并打印。

---

## 三、压测横评结果（相同数据对比）

> 规模：2 万行 × 7 列订单。**导出**用同一份数据喂 6 个写方案；**导入**读同一个 `big-orders.xlsx` 用 4 个读方案。
> 计时/内存为 JVM 级近似（先 `System.gc()`+sleep 再取差值），随机器/磁盘波动，故测试只断言"横评跑完 + 报告生成"，不硬断言数字。

### 导出：耗时(ms) / 内存增量(MB) / 文件大小

| 方案 | 耗时(ms) | 内存增量(MB) | 文件大小 |
| --- | --- | --- | --- |
| XSSF（全内存） | 3471 | 174.5 | 619.3 KB |
| SXSSF（窗口=100） | 6063 | 106.5 | 607.4 KB |
| SXSSF（窗口=1000） | 6056 | 243.7 | 607.5 KB |
| EasyExcel | 626 | 82.4 | 627.1 KB |
| CSV（EasyExcel） | 307 | 66.4 | 1.28 MB |
| **Hutool（全内存封装）** | **5329** | **392.2** | 619.5 KB |

### 导入：耗时(ms) / 内存增量(MB) / 读到行数

| 方案 | 耗时(ms) | 内存增量(MB) | 读到行数 |
| --- | --- | --- | --- |
| 用户模型（全内存） | 714 | 98.7 | 20000 |
| SAX 事件模型 | 443 | 61.0 | 20000 |
| EasyExcel 监听器（每批 1000） | 279 | 10.4 | 20000 |
| **Hutool 读（全内存封装）** | **739** | **290.8** | 20000 |

---

## 四、关键结论

1. **正确性**：16 个用例全部覆盖"写→读往返"或"版本约束"的强验证，无脆弱的环境相关断言（体积差异等交给 bench 实测）。
2. **版本安全**：POI 统一锁 5.5.1、EasyExcel 4.0.3、commons-compress 1.28.0、commons-io 2.21.0——既防运行期 `NoSuchMethodError`，也防 Spring Boot BOM 吞掉 zip 安全修复。
3. **性能选型**：
   - 导出：**EasyExcel / CSV 最快最省**；XSSF 内存随行数线性涨是 OOM 头号元凶；**Hutool 全内存封装最慢最吃内存**（392 MB），印证"底层仍是 POI 全内存"。
   - 导入：**EasyExcel 监听器综合最优**（279 ms / 10.4 MB）；SAX 次之；**Hutool 读与用户模型均为全内存，Hutool 内存更高（290.8 MB）**。
   - 所有导入方案均读回 **20000** 行，确认非空跑、数据完整。

---

## 五、如何复现

```bash
# 默认 2 万行横评，生成 target/bench-results.md 与本报告
mvn test

# 拉大到 10 万行看大文件下内存分野（尤其 SAX vs 全内存）
mvn test -Dbench.rows=100000

# 只跑横评（更快）
mvn test -Dtest=ExcelBenchTest
```

产物：
- 横评明细：`target/bench-results.md`
- 本综合报告：`target/test-report.md`（本文件为模块根副本，便于入库）
- Surefire 明细：`target/surefire-reports/*.txt`（或 `*.xml`）
