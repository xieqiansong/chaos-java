package lan.chaos.word;

import lan.chaos.word.basic.BasicWordService;
import lan.chaos.word.bigdoc.BigDocService;
import lan.chaos.word.common.constant.WordConstants;
import lan.chaos.word.common.config.WordProperties;
import lan.chaos.word.common.util.OutFiles;
import lan.chaos.word.read.ReadService;
import lan.chaos.word.style.StyleService;
import lan.chaos.word.table.TableService;
import lan.chaos.word.template.TemplateFillService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Word 专题场景测试：每个能力都「生成 → 用 POI 重新读回 → 断言关键结果」，
 * 不依赖肉眼打开（但产物都真实落盘到 target/out，可人工打开验证）。
 */
class WordScenarioTest {

    private final OutFiles outFiles = new OutFiles(new WordProperties());
    private final BasicWordService basic = new BasicWordService(outFiles);
    private final StyleService style = new StyleService(outFiles);
    private final TableService table = new TableService(outFiles);
    private final ReadService read = new ReadService(outFiles);
    private final TemplateFillService template = new TemplateFillService(outFiles);
    private final BigDocService bigdoc = new BigDocService(outFiles);

    @Test
    void basic_generatesStructuredDoc() throws Exception {
        String out = basic.run();
        File file = outFiles.of("word-basic.docx");
        assertTrue(file.exists(), "产物应已落盘");
        assertTrue(out.contains("页眉=机密"), "应有页眉");
        assertTrue(out.contains("页脚=仅供内部参考"), "应有页脚");
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            assertTrue(doc.getParagraphs().size() > 5, "应有多个段落");
            assertEquals(1, doc.getTables().size(), "应有一张表格");
        }
    }

    @Test
    void style_eastAsiaPitExistsAndFixWorks() throws Exception {
        style.build();
        File file = outFiles.of("word-style.docx");
        boolean hasEastAsia = false;
        boolean hasEmpty = false;
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                for (XWPFRun run : p.getRuns()) {
                    if (run.getCTR().isSetRPr()) {
                        CTRPr rPr = run.getCTR().getRPr();
                        // POI 5.5.1：CTRPr 里 rFonts 以数组形式暴露，用 sizeOfRFontsArray/getRFontsArray(0)
                        if (rPr.sizeOfRFontsArray() > 0) {
                            String ea = rPr.getRFontsArray(0).getEastAsia();
                            if ("宋体".equals(ea)) {
                                hasEastAsia = true;
                            }
                            if (ea == null) {
                                hasEmpty = true;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(hasEastAsia, "setCjkFont 应设置 EastAsia=宋体");
        assertTrue(hasEmpty, "只 setFontFamily 的段落 EastAsia 应为空（坑确实存在）");
    }

    @Test
    void table_mergeMarkersCorrect() throws Exception {
        table.build();
        File file = outFiles.of("word-table.docx");
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            XWPFTable t = doc.getTables().get(0);
            assertEquals(4, t.getRows().size(), "应有 4 行");
            assertEquals("restart",
                    t.getRow(0).getCell(0).getCTTc().getTcPr().getHMerge().getVal().toString(),
                    "A1 横向合并应为 restart");
            assertEquals("restart",
                    t.getRow(1).getCell(0).getCTTc().getTcPr().getVMerge().getVal().toString(),
                    "A2 纵向合并应为 restart");
        }
    }

    @Test
    void read_docx() throws Exception {
        read.run();
        File docx = outFiles.of("word-read.docx");
        assertTrue(docx.exists());
        try (XWPFDocument d = new XWPFDocument(new FileInputStream(docx))) {
            assertTrue(d.getTables().get(0).getRow(0).getCell(0).getText().contains("姓名"),
                    "应能读回表格内容");
        }
    }

    @Test
    void template_crossRunPitAndTableCopy() throws Exception {
        template.run();
        File naive = outFiles.of("word-template-naive.docx");
        File correct = outFiles.of("word-template-filled.docx");

        // 朴素替换：author 跨 run 割裂，占位符应仍然存在
        boolean naiveStillHasPlaceholder = false;
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(naive))) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                if (p.getText() != null && p.getText().contains("${author}")) {
                    naiveStillHasPlaceholder = true;
                }
            }
        }
        assertTrue(naiveStillHasPlaceholder, "朴素逐 run 替换应失败，占位符 ${author} 仍在");

        // 正确替换：合并 run 后 author 变为真实值，且表格行复制成功
        boolean correctReplaced = false;
        int dataRows = 0;
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(correct))) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                if (p.getText() != null && p.getText().contains("张三")) {
                    correctReplaced = true;
                }
            }
            dataRows = doc.getTables().get(0).getRows().size();
        }
        assertTrue(correctReplaced, "合并 run 替换应成功，author 变为张三");
        // 表头 + 3 条数据 = 4 行
        assertEquals(4, dataRows, "模板行复制应生成 1 表头 + 3 数据行");
    }

    @Test
    void bigdoc_generatesWithoutError() throws Exception {
        BigDocService.Result r = bigdoc.generate(WordConstants.DEFAULT_SIZE);
        assertTrue(r.file.exists());
        assertFalse(r.elapsed < 0);
    }
}
