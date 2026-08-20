package lan.chaos.starter.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lan.chaos.starter.token.TokenProperties;
import lan.chaos.starter.token.TokenService;

/**
 * Spring Boot Starter 自动装配核心配置类（制造方）。
 *
 * <p>WHY（自动装配解决什么痛点）：
 * 没有 starter 时，用户要自己写 @Configuration、自己 new TokenService、自己处理依赖顺序。
 * 有了 starter，用户只需要在 pom 引依赖 + 在 application.yml 写配置，Bean 就自动就绪。</p>
 *
 * <p>关键机制（逐条对应注解）：
 * 1. {@link Configuration}：声明这是一个配置类。
 * 2. {@link EnableConfigurationProperties}：把 {@link TokenProperties} 注册为 Bean，
 *    并触发其与配置文件的绑定（prefix=token.starter）。
 * 3. {@link ConditionalOnProperty}：当 token.starter.enabled=true（默认）才装配，
 *    用户设 false 即可整体关闭——这是「可开关」的标准姿势。
 * 4. {@link ConditionalOnMissingBean}：仅当用户「没有」自己定义 TokenService 时才装配默认实现，
 *    允许用户用自定义 Bean 覆盖 starter 默认行为（开放封闭原则）。</p>
 *
 * <p>生产坑：
 * - 自动配置类本身【不要】加 @ComponentScan 能扫到的包，否则失去「按需装配」意义；
 *   它只通过 META-INF/spring/...AutoConfiguration.imports 被 Spring Boot 主动加载。
 * - 多个自动配置之间有顺序依赖时用 @AutoConfigureAfter / @AutoConfigureBefore。</p>
 */
@Configuration
@EnableConfigurationProperties(TokenProperties.class)
@ConditionalOnProperty(prefix = "token.starter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TokenAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TokenService tokenService(TokenProperties properties) {
        return new TokenService(properties);
    }
}
