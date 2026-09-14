package lan.chaos.starter.usage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lan.chaos.starter.token.TokenService;

/**
 * Starter 使用方（消费应用）：演示「引依赖即用」。
 *
 * <p>注意：本类位于 starter 同模块内，仅为方便演示。真实场景下，制造方（autoconfigure +
 * TokenService）会被打成一个独立的 token-spring-boot-starter.jar，使用方只需：
 *   1) pom 引入该 starter 依赖
 *   2) application.yml 写 token.starter.* 配置
 *   3) 像下面这样直接 @Autowired TokenService（无需任何手动 @Bean 声明）
 * 这就是 starter 的全部魅力——用户侧零配置装配。</p>
 */
@SpringBootApplication
public class StarterUsageApplication implements CommandLineRunner {

    @Autowired
    private TokenService tokenService;

    public static void main(String[] args) {
        SpringApplication.run(StarterUsageApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // 输入（配置）→ 输出（token），控制台可观察
        System.out.println("=== Starter 使用方启动验证 ===");
        System.out.println("生效配置: " + tokenService.getProperties());
        System.out.println("生成 token: " + tokenService.generate());
    }
}
