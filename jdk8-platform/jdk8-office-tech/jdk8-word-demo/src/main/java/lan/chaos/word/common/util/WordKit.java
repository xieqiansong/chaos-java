package lan.chaos.word.common.util;

import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;

import java.math.BigInteger;

/**
 * XWPF 的「踩坑才学到」底层 API 集中放这里：全模块共用，复制即用。
 *
 * <p>WHY 单列一个工具类：XWPF 的「样式 / 中文字体 / 合并单元格」都必须下钻到 XML Beans（CT* 类），
 * 而这些调用散落在各 service 里会既重复又易错。把硬核部分收口到一处，
 * 既降低出错面，也让读者一眼看清「哪些是 POI 的原生坑」。
 */
public final class WordKit {

    private WordKit() {
    }

    /**
     * 同时设置 Ascii 与 EastAsia（中文）字体。
     *
     * <p><b>经典坑</b>：{@code run.setFontFamily("宋体")} 只设置了 Ascii/HAnsi 字体槽位，
     * 中文字符实际走的是 EastAsia 槽位。只设前者，中文在 WPS/Word 里会回退成默认字体甚至方块。
     * 必须显式 {@code rFonts.setEastAsia(...)} 才真正生效。
     */
    public static void setCjkFont(XWPFRun run, String font) {
        run.setFontFamily(font); // 设 Ascii/HAnsi
        CTRPr rPr = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        // 注意：POI 5.5.1 的 CTRPr 里 rFonts 以数组形式暴露（无单数 isSetRFonts/getRFonts），
        // 必须用 sizeOfRFontsArray / getRFontsArray(0) / addNewRFonts。
        CTFonts fonts = rPr.sizeOfRFontsArray() > 0 ? rPr.getRFontsArray(0) : rPr.addNewRFonts();
        fonts.setEastAsia(font); // 中文真正用到的字体槽位
    }

    /**
     * 在文档中创建（若不存在）一个段落样式，供 {@code paragraph.setStyle(id)} 引用。
     *
     * <p><b>坑</b>：新建的 {@code XWPFDocument} 里没有任何样式定义，
     * 直接 {@code setStyle("Heading1")} 只是写了个引用而样式不存在，部分阅读器会忽略。
     * 必须先用 CTStyle 把样式真正写进文档的 styles.xml。
     */
    public static void ensureParagraphStyle(XWPFDocument doc, String styleId, int halfPt, String color) {
        XWPFStyles styles = doc.createStyles();
        if (styles.getStyle(styleId) != null) {
            return;
        }
        CTStyle ct = CTStyle.Factory.newInstance();
        ct.setStyleId(styleId);
        ct.setType(STStyleType.PARAGRAPH);
        CTRPr rPr = ct.addNewRPr();
        rPr.addNewSz().setVal(BigInteger.valueOf(halfPt));
        if (color != null) {
            rPr.addNewColor().setVal(color);
        }
        styles.addStyle(new XWPFStyle(ct));
    }

    /** 插入分页符。 */
    public static void pageBreak(XWPFRun run) {
        run.addBreak(BreakType.PAGE);
    }

    /**
     * 水平合并单元格：对「起始格」用 {@link STMerge#RESTART}，后续被合并格用 {@link STMerge#CONTINUE}。
     */
    public static void mergeHorizontal(XWPFTableCell cell, String mode) {
        CTTcPr tcPr =
                cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTHMerge merge = tcPr.isSetHMerge() ? tcPr.getHMerge() : tcPr.addNewHMerge();
        merge.setVal("restart".equalsIgnoreCase(mode) ? STMerge.RESTART : STMerge.CONTINUE);
    }

    /** 垂直合并单元格：同上，起始格 RESTART，后续 CONTINUE。 */
    public static void mergeVertical(XWPFTableCell cell, String mode) {
        CTTcPr tcPr =
                cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTVMerge merge = tcPr.isSetVMerge() ? tcPr.getVMerge() : tcPr.addNewVMerge();
        merge.setVal("restart".equalsIgnoreCase(mode) ? STMerge.RESTART : STMerge.CONTINUE);
    }

    /** 往单元格写文本（不依赖可能变更的 setText 行为，统一走段落 run）。 */
    public static void setCellText(XWPFTableCell cell, String text) {
        cell.addParagraph().createRun().setText(text);
    }
}
