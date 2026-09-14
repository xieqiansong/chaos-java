package lan.chaos.word.common.constant;

/** 本 Demo 的公共常量：示例规模、占位符、页眉等。杜绝魔法值。 */
public final class WordConstants {

    /** 功能演示场景的默认规模（段落数 / 表格行数）。 */
    public static final int DEFAULT_SIZE = 50;

    /** 压测场景默认段落数（2 万段：足以看到 XWPF 全内存模型的耗时与内存峰值）。 */
    public static final int BENCH_PARAGRAPHS = 20_000;

    /** 模板占位符前缀/后缀（演示替换用）。 */
    public static final String PH_OPEN = "${";
    public static final String PH_CLOSE = "}";

    /** 生成的示例图片尺寸（EMU：English Metric Units，1 inch = 914400 EMU）。 */
    public static final int IMAGE_WIDTH_EMU = 360_000;   // 约 3.9 cm
    public static final int IMAGE_HEIGHT_EMU = 200_000;  // 约 2.2 cm

    /** 默认中文字体（EastAsia）。 */
    public static final String CJK_FONT = "宋体";

    /** 默认中文标题字体。 */
    public static final String CJK_TITLE_FONT = "黑体";

    private WordConstants() {
    }
}
