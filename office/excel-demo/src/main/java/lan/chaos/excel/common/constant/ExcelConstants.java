package lan.chaos.excel.common.constant;

/** 本 Demo 的公共常量：sheet 名、表头、场景规模、POI 的硬性上限。杜绝魔法值。 */
public final class ExcelConstants {

    /** 订单 sheet 名。 */
    public static final String SHEET_ORDERS = "订单";

    /** 订单表头（与 Order 的 @ExcelProperty 一一对应）。 */
    public static final String[] HEADERS = {"订单号", "客户", "商品", "数量", "金额", "状态", "下单时间"};

    /** 功能演示场景的默认行数（跑得快，用于单元测试）。 */
    public static final int DEFAULT_ROWS = 1_000;

    /**
     * 压测场景默认行数（2 万行）：保证 CI 上也能稳定跑完（XSSF 全内存方案是内存大户）。
     * 想要更大规模对比时用 <code>-Dbench.rows=100000</code> 调大。
     */
    public static final int BENCH_ROWS = 20_000;

    /** HSSF（.xls, Excel 97-2003）的硬上限：65536 行 × 256 列。 */
    public static final int HSSF_MAX_ROWS = 65_536;

    /** XSSF/SXSSF（.xlsx, Excel 2007+）的单表上限：1048576 行。 */
    public static final int XSSF_MAX_ROWS = 1_048_576;

    /** 一个 Workbook 最多能创建的 CellStyle 数量（Excel 规范上限，超了抛异常）。 */
    public static final int CELL_STYLE_LIMIT = 64_000;

    /** SXSSF 默认滑动窗口大小（POI 默认 100）。 */
    public static final int SXSSF_DEFAULT_WINDOW = 100;

    private ExcelConstants() {
    }
}
