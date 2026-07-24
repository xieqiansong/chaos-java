package lan.chaos.microservice.common.core.constant;

/**
 * 多数据源名称常量。
 *
 * <p>WHY：{@code @DS} 注解里的字符串容易拼错且散落各处。集中成常量，
 * 与 application-common.yml 中 {@code spring.datasource.dynamic.datasource.*} 的 key 一一对应，
 * 改名字时编译期即可发现遗漏。</p>
 */
public final class DataSourceConstants {

    /** 主数据源：PostgreSQL（业务主库） */
    public static final String PG = "pg";

    /** 第二数据源：MySQL（演示异构数据源切换） */
    public static final String MYSQL = "mysql";

    private DataSourceConstants() {
    }
}
