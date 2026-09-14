package lan.chaos.pdf.common.constant;

/** 全模块共用的常量：页面几何、字号、中文字体候选路径、压测规模。 */
public final class PdfConstants {

    private PdfConstants() {
    }

    /** 页面左右边距（pt，1pt = 1/72 英寸）。 */
    public static final float MARGIN_X = 50f;

    /** 页面上边距。 */
    public static final float MARGIN_TOP = 60f;

    /** 页面下边距：低于此 Y 值必须换页，否则文字会画到页面外（PDF 不会自动换行/分页）。 */
    public static final float MARGIN_BOTTOM = 60f;

    /** 标题字号。 */
    public static final float TITLE_SIZE = 18f;

    /** 小标题字号。 */
    public static final float HEADING_SIZE = 13f;

    /** 正文字号。 */
    public static final float BODY_SIZE = 11f;

    /** 表格字号。 */
    public static final float TABLE_SIZE = 10f;

    /** 正文行高（= 字号 × 1.5，够宽松，中文不会挤在一起）。 */
    public static final float LINE_HEIGHT = 16.5f;

    /** 表格行高。 */
    public static final float ROW_HEIGHT = 20f;

    /**
     * 中文字体候选路径（按优先级）。
     *
     * <p>WHY 要做「探测」而不是随包带一个字体：中文字体（思源黑体等）动辄 10~20 MB，
     * 且大多有独立授权条款，塞进代码仓库既不合理也不合规。生产做法是把字体当部署物
     * （Docker 镜像里 apt install fonts-noto-cjk，或挂到固定路径），代码里只做探测。
     *
     * <p>注意 .ttc 是「字体集合」（TrueType Collection），一个文件里有多个字体，
     * PDFBox 必须先用 {@code TrueTypeCollection} 拆包，不能直接 {@code PDType0Font.load}。
     */
    public static final String[] CJK_FONT_CANDIDATES = {
            // Windows：优先挑 .ttf（单字体文件，最直接）
            "C:/Windows/Fonts/simhei.ttf",   // 黑体
            "C:/Windows/Fonts/simkai.ttf",   // 楷体
            "C:/Windows/Fonts/simfang.ttf",  // 仿宋
            "C:/Windows/Fonts/msyh.ttc",     // 微软雅黑（TTC 集合）
            "C:/Windows/Fonts/simsun.ttc",   // 宋体（TTC 集合）
            // Linux（常见发行版）
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/arphic/uming.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            // macOS
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/STHeiti Medium.ttc",
    };

    /** TTC 集合里要取的子字体名（不同系统字体名不同，逐个尝试）。 */
    public static final String[] TTC_FONT_NAMES = {"SimHei", "SimSun", "Microsoft YaHei", "KaiTi", "FangSong"};

    /** 大文档压测：默认生成的页数。 */
    public static final int BENCH_PAGES = 30;

    /**
     * 大文档压测：每轮写入的正文行数。
     *
     * <p>取值有讲究：A4 可用高度 = 842 - 上边距 60 - 下边距 60 = 722pt；
     * 一轮内容 = 1 行小标题(13×1.5=19.5) + n 行正文(11×1.5=16.5)。
     * 令 19.5 + n×16.5 ≤ 722 → n ≤ 42.5，取 42 可保证「一轮 ≈ 一页」，
     * 否则每轮都会被 PdfCanvas 自动分页打断，导致「请求 30 页」实际排出 30+ 页。
     */
    public static final int BENCH_LINES_PER_PAGE = 42;

    /** 压测正文样例（重复用，制造足够体量的文本）。 */
    public static final String BENCH_TEXT =
            "PDF 没有流式布局引擎：每一页、每一行、每个字的位置都必须由程序自己算出来，"
                    + "这一点和 HTML/Word 的世界观完全不同。本行用于观察 PDFBox 全内存模型的耗时与内存增长。";
}
