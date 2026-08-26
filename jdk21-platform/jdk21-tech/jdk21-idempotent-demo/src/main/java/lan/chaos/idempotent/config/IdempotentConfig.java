package lan.chaos.idempotent.config;

import org.springframework.context.annotation.Configuration;

/**
 * 幂等模块装配配置。无外部中间件，仅依赖 Spring JDBC 自动装配的 JdbcTemplate。
 * 去重表在 {@link lan.chaos.idempotent.core.H2IdempotencyStore} 构造时建表。
 */
@Configuration
public class IdempotentConfig {
}
