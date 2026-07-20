package lan.chaos.mybatisplus.common.context;

/**
 * 动态表名上下文（ThreadLocal 传递分表后缀）。
 * 例如把 log_record 按年份路由到 log_record_2024 / log_record_2025。
 */
public class DynamicTableContext {
    private static final ThreadLocal<String> TL = new ThreadLocal<>();

    public static void setSuffix(String suffix) {
        TL.set(suffix);
    }

    public static String getSuffix() {
        return TL.get();
    }

    public static void clear() {
        TL.remove();
    }
}
