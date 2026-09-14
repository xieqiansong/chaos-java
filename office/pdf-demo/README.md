# jdk8-pdf-demo

PDF 导入导出 Demo：基于 **Apache PDFBox 3.0.x**，覆盖结构化文档绘制、中文字体嵌入（最大坑）、表格、内容提取、合并拆分、大文档内存横评。

## 技术栈

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 8 | |
| Spring Boot | 平台 BOM | 仅用于装配 |
| Apache PDFBox | **3.0.x** | 含 fontbox / pdfbox-io（同版本，由平台 BOM 收敛）|
| 测试 | JUnit 5 + Spring Boot Test | 本模块暂无自动化测试，仅 DemoApp 演示 |

## 快速开始

```bash
# 在模块目录执行

mvn test                                            # 编译 + 校验
mvn exec:java -Dexec.mainClass=lan.chaos.pdf.DemoApp     # 分节打印每个能力（0 ~ 6）

# 大文档内存横评（默认页数，可调）
mvn exec:java -Dexec.mainClass=lan.chaos.pdf.DemoApp -Dbench.pages=500
```

## 能力清单（对应 DemoApp 0 ~ 6 节）

| 节 | 能力 | 要点 |
| --- | --- | --- |
| 0 | 依赖版本 | 运行期实际生效的 pdfbox / fontbox / pdfbox-io |
| 1 | **中文字体嵌入（PDF 第一大坑）** | 标准 14 字体写中文直接抛异常；`PDType0Font.load` 嵌入真实字体；`.ttc` 集合需拆包；子集化；字体探测与失败提示 |
| 2 | 结构化文档绘制 | 坐标系（原点在左下）、自动分页 |
| 3 | 表格绘制 | PDF **没有表格对象**，全部用线条 + 文本画出来 |
| 4 | 内容提取（PDFTextStripper 乱序坑） | `setSortByPosition` 排序 vs 默认内容流顺序；表格 / 扫描件不可靠 |
| 5 | 合并与拆分 | `PDDocument` 合并、按页码范围拆分 |
| 6 | 大文档内存模型横评 | 不同构造策略的内存对比 |

## 关键技术坑（摘要，详见各 Service 类注释）

- **PDFBox 3.0 破坏性变更**（旧博客会编译不过）：
  - 读取入口 `PDDocument.load(...)` → `Loader.loadPDF(...)`；
  - `PDPageContentStream` 新增 `(doc, page)` 两参构造；
  - 标准字体 `PDType1Font.HELVETICA` 常量已移除 → `new PDType1Font(Standard14Fonts.FontName.HELVETICA)`；
  - 异常文案变化：2.x `No glyph for U+4E2D` → 3.0 `... is not available in the font ...`，断言应匹配码点而非整句。
- **中文字体**：标准 14 字体无任何中文字形；必须嵌入真实字体；字体文件 10~20MB 且授权各异，**不能随包**，应作部署物；全量嵌入致 PDF 暴涨，需**子集化**（通常降到几十 KB）；容器镜像漏装字体是线上最常见事故，须有明确失败提示。
- **内容提取**：PDF 是「版面格式」而非「结构化文档」，无段落 / 表格 / 阅读顺序；默认按内容流书写顺序输出，双栏 / 表格必然乱序；开启排序只是按 (y,x) 排序，旋转页 / 跨页多栏仍会串；**表格提取基本不可靠**，数据交换别选 PDF；扫描件需先 OCR。
- **文本测量 / 坐标**：中文宽度需字体 `getStringWidth`；坐标系 y 轴向下需换算。

## 产物

所有演示产物在 `target/out/`，可用任意 PDF 阅读器打开验证。
