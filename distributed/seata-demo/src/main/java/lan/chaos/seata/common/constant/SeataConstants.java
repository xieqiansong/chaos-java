package lan.chaos.seata.common.constant;

/**
 * Seata Demo 统一常量。
 * <p>
 * 将所有 user_id、product_id、金额、库存数量等魔法值集中管理，
 * 避免各 Service 里散落硬编码。
 * </p>
 *
 * @author chaos
 */
public final class SeataConstants {

    private SeataConstants() {
    }

    // ============ 测试数据标识 ============
    /** 测试用户 */
    public static final String USER_ID = "U1001";
    /** 测试商品 */
    public static final String PRODUCT_ID = "P1001";

    // ============ 金额与库存 ============
    /** 初始账户余额 */
    public static final double INIT_BALANCE = 1000.0;
    /** 初始库存数量 */
    public static final int INIT_STOCK = 100;
    /** 单次购买金额 */
    public static final double ORDER_AMOUNT = 100.0;
    /** 单次扣减库存 */
    public static final int ORDER_COUNT = 1;

    // ============ AT 模式 ============
    /** AT 模式用户余额不足时触发的回滚场景 */
    public static final String USER_INSUFFICIENT = "U9999";

    // ============ TCC 模式业务动作名 ============
    /** TCC 扣款 action */
    public static final String TCC_ACTION_ACCOUNT = "account-deduct";
    /** TCC 下单 action */
    public static final String TCC_ACTION_ORDER = "order-create";
    /** TCC 扣库存 action */
    public static final String TCC_ACTION_STORAGE = "storage-deduct";
}
