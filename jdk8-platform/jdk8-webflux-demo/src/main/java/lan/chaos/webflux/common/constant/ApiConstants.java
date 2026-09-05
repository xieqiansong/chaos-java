package lan.chaos.webflux.common.constant;

/**
 * 路由/路径常量，避免魔法值。
 * 函数式路由与注解式 Controller 使用不同前缀，避免同路径歧义（二者可共存）。
 */
public final class ApiConstants {
    private ApiConstants() {
    }

    /** 函数式路由(RouterFunction)使用的路径。 */
    public static final String PRODUCTS = "/api/products";
    public static final String PRODUCT_BY_ID = "/api/products/{id}";

    /** 注解式响应式 Controller 使用的路径（与函数式路由区分）。 */
    public static final String ANNOTATED_PRODUCTS = "/api/annotated/products";
    public static final String ANNOTATED_PRODUCT_BY_ID = "/api/annotated/products/{id}";
}
