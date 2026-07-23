package lan.chaos.mybatisplus.common.context;

/**
 * 多租户上下文（ThreadLocal 传递当前租户 ID）。
 * 真实环境一般从登录态 / 请求头解析后写入，这里用静态方法演示手工切换。
 */
public class TenantContext {
    private static final ThreadLocal<Long> TL = new ThreadLocal<>();

    public static void set(Long tenantId) {
        TL.set(tenantId);
    }

    public static Long get() {
        return TL.get();
    }

    public static void clear() {
        TL.remove();
    }
}
