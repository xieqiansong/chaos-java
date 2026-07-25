package lan.chaos.microservice.user.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P6 动态刷新单测（离线、无 Nacos）。
 *
 * <p>用 {@link ApplicationContextRunner} 起一个最小 Spring 上下文，验证：
 * <ol>
 *   <li>{@link DynamicConfig} 通过 {@code @ConfigurationProperties} 正确绑定 {@code ms.config.greeting}；</li>
 *   <li>在 Environment 中“模拟 Nacos 发布新值”后，调用 {@link RefreshScope#refreshAll()}（等价于收到 refresh 事件），
 *       再次取出的 Bean 是<b>重建后的新实例</b>且值为新值——即“改配置热更生效、无需重启”。</li>
 * </ol>
 */
class DynamicConfigRefreshTest {

    @Configuration
    @EnableConfigurationProperties
    static class TestConfig {
        // refresh 作用域：@RefreshScope 的 Bean 由它管理，refreshAll() 清空缓存触发重建
        @Bean
        public RefreshScope refreshScope() {
            return new RefreshScope();
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DynamicConfig.class, TestConfig.class)
            .withPropertyValues(
                    "spring.cloud.nacos.config.enabled=false",
                    "ms.config.greeting=V1");

    @Test
    void greetingBindsFromConfigAndRefreshesWithoutRestart() {
        runner.run(context -> {
            DynamicConfig before = context.getBean(DynamicConfig.class);
            assertThat(before.getGreeting()).isEqualTo("V1");

            // 模拟“在 Nacos 修改 ms.config.greeting 并发布”
            ConfigurableEnvironment env = context.getEnvironment();
            Map<String, Object> override = new HashMap<>();
            override.put("ms.config.greeting", "V2-hot-reloaded");
            env.getPropertySources().addFirst(new MapPropertySource("nacos-override", override));

            // Spring Cloud 收到 refresh 事件会销毁重建 @RefreshScope Bean；这里直接调用 refreshAll 复现
            RefreshScope refreshScope = context.getBean(RefreshScope.class);
            refreshScope.refreshAll();

            // @RefreshScope 返回的是同一个代理对象，但 refreshAll() 后其内部目标已被销毁重建，
            // 再次取值会从新目标读取——这才是“热更”的验证点（比较值而非代理身份）
            DynamicConfig after = context.getBean(DynamicConfig.class);
            assertThat(after).isSameAs(before);                    // 代理是同一个单例
            assertThat(after.getGreeting()).isEqualTo("V2-hot-reloaded"); // 但内部值已热更
        });
    }
}
