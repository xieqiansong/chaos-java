package lan.chaos.sentinel.common.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentienl 切面配置 — 显式注册 {@link SentinelResourceAspect} 确保 @SentinelResource 注解生效。
 *
 * <p>Spring Cloud Alibaba 的 Sentinel 自动配置理论上会自动注册该切面，
 * 但在某些测试场景下可能失效；显式声明保证行为可控。</p>
 */
@Configuration
public class SentinelAspectConfig {

    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
}
