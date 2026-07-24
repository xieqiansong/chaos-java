package lan.chaos.microservice.common.security.annotation;

/**
 * 多权限逻辑关系（配合 {@link RequiresPermission}）。
 */
public enum Logical {
    /** 全部满足。 */
    AND,
    /** 满足其一。 */
    OR
}
