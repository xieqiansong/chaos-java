package lan.chaos.starter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import lan.chaos.starter.autoconfigure.TokenAutoConfiguration;
import lan.chaos.starter.token.TokenProperties;
import lan.chaos.starter.token.TokenService;

/**
 * Starter 自动装配单元测试（首要验证形态，可断言、零外部依赖）。
 *
 * <p>使用 {@link ApplicationContextRunner} 而非启动整个 SpringApplication，
 * 精准验证「自动配置类在何种条件下装配/不装配 TokenService」——这正是 starter 的核心契约。</p>
 */
class TokenAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TokenAutoConfiguration.class));

    /** 场景1：默认情况下 TokenService 应被自动装配，且配置绑定到默认值。 */
    @Test
    void shouldAutoConfigureTokenServiceByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(TokenService.class);
            TokenProperties props = context.getBean(TokenProperties.class);
            assertThat(props.getLength()).isEqualTo(32);
            assertThat(props.getCharset()).isEqualTo(TokenProperties.Charset.SIMPLE);
            // 输入→输出可观察：生成的 token 长度应等于配置长度
            String token = context.getBean(TokenService.class).generate();
            assertThat(token).hasSize(props.getLength());
        });
    }

    /** 场景2：用户通过配置覆盖 TokenProperties（prefix 绑定生效）。 */
    @Test
    void shouldBindCustomProperties() {
        runner.withPropertyValues("token.starter.length=16", "token.starter.prefix=tk_")
                .run(context -> {
                    TokenProperties props = context.getBean(TokenProperties.class);
                    assertThat(props.getLength()).isEqualTo(16);
                    assertThat(props.getPrefix()).isEqualTo("tk_");
                    String token = context.getBean(TokenService.class).generate();
                    assertThat(token).startsWith("tk_").hasSize("tk_".length() + 16);
                });
    }

    /** 场景3：token.starter.enabled=false 时完全不装配 TokenService。 */
    @Test
    void shouldNotConfigureWhenDisabled() {
        runner.withPropertyValues("token.starter.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TokenService.class));
    }

    /** 场景4：用户自定义 TokenService 时，starter 默认实现不覆盖（@ConditionalOnMissingBean）。 */
    @Test
    void shouldHonorUserDefinedBean() {
        runner.withUserConfiguration(UserConfig.class)
                .run(context -> {
                    TokenService svc = context.getBean(TokenService.class);
                    assertThat(svc.generate()).startsWith("[USER]");
                });
    }

    static class UserConfig {
        @org.springframework.context.annotation.Bean
        TokenService tokenService(TokenProperties properties) {
            return new TokenService(properties) {
                @Override
                public String generate() {
                    return "[USER]" + super.generate();
                }
            };
        }
    }
}
