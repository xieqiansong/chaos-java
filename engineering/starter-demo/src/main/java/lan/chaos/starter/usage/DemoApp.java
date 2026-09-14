package lan.chaos.starter.usage;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import lan.chaos.starter.autoconfigure.TokenAutoConfiguration;
import lan.chaos.starter.token.TokenProperties;
import lan.chaos.starter.token.TokenService;

/**
 * 控制台一键演示：不依赖 Web，分节打印「自动装配机制」各场景的输入→输出。
 */
public class DemoApp {

    private static final String LINE = "--------------------------------------------------";

    public static void main(String[] args) {
        defaultAssembly();
        customOverride();
        disabledByProperty();
    }

    /** 场景一：默认装配（用户什么都不配，拿到的就是默认 Bean）。 */
    private static void defaultAssembly() {
        System.out.println(LINE);
        System.out.println("[场景1] 默认装配：启用开关开启、无用户自定义 Bean");
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(TokenAutoConfiguration.class)) {
            TokenService svc = ctx.getBean(TokenService.class);
            System.out.println("  输入(默认配置): " + svc.getProperties());
            System.out.println("  输出(token)   : " + svc.generate());
        }
        System.out.println(LINE);
    }

    /** 场景二：用户自定义 Bean 覆盖 starter 默认实现（@ConditionalOnMissingBean 生效）。 */
    private static void customOverride() {
        System.out.println("[场景2] 用户覆盖：提供自己的 TokenService 后，starter 默认实现不生效");
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext()) {
            ctx.register(TokenAutoConfiguration.class);
            ctx.register(CustomTokenConfig.class);
            ctx.refresh();
            TokenService svc = ctx.getBean(TokenService.class);
            System.out.println("  输出(token)   : " + svc.generate() + "  (来自用户自定义实现)");
        }
        System.out.println(LINE);
    }

    /** 场景三：enabled=false 时 starter 完全不装配。 */
    private static void disabledByProperty() {
        System.out.println("[场景3] 关闭开关：token.starter.enabled=false 时不提供 TokenService");
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().getPropertySources().addFirst(
                    new org.springframework.core.env.MapPropertySource("test",
                            java.util.Collections.singletonMap("token.starter.enabled", "false")));
            ctx.register(TokenAutoConfiguration.class);
            ctx.refresh();
            boolean exists = ctx.containsBean("tokenService");
            System.out.println("  容器中存在 tokenService Bean? " + exists + "  (应为 false)");
        }
        System.out.println(LINE);
    }

    /** 用户侧自定义实现，用于演示 @ConditionalOnMissingBean 被覆盖。 */
    static class CustomTokenConfig {
        @org.springframework.context.annotation.Bean
        public TokenService tokenService(TokenProperties properties) {
            return new TokenService(properties) {
                @Override
                public String generate() {
                    return "[CUSTOM]" + super.generate();
                }
            };
        }
    }
}
