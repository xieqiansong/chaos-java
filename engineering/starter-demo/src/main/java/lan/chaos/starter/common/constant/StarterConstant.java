package lan.chaos.starter.common.constant;

/**
 * Starter 约定相关常量（杜绝魔法值）。
 *
 * <p>WHY：Spring Boot 官方对「第三方 starter」与「官方 starter」的命名有明确约定，
 * 这是面试与生产中极易踩坑的细节，集中在此讲清。</p>
 */
public final class StarterConstant {

    private StarterConstant() {
    }

    /**
     * 配置前缀：第三方 starter 推荐用「业务名」前缀（非 spring.），避免与官方冲突。
     * 例：token.starter.enabled / token.starter.length。
     */
    public static final String CONFIG_PREFIX = "token.starter";

    /**
     * 自动配置文件路径（Spring Boot 2.7 推荐）：
     * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
     * ——取代老旧的 META-INF/spring.factories。
     */
    public static final String AUTO_CONFIG_FILE =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    /**
     * 命名约定：
     * - 官方 starter：spring-boot-starter-xxx（如 spring-boot-starter-web）
     * - 第三方 starter：xxx-spring-boot-starter（如 mybatis-plus-spring-boot-starter）
     * 本 demo 起名 token-spring-boot-starter 即遵循后者。
     */
    public static final String THIRD_PARTY_NAMING = "xxx-spring-boot-starter";

    public static final String OFFICIAL_NAMING = "spring-boot-starter-xxx";
}
