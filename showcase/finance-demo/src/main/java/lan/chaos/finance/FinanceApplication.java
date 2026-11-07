package lan.chaos.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 金融支付清结算与账务核心 · 综合实战入口。
 *
 * <p>非 Web 启动：仅承载各能力包的 {@code ApplicationRunner} 控制台演示。
 * 真正的"可观察输出"与断言由 {@code src/test} 下的 {@code *Test} 保证（见 AGENTS.md 第四章）。
 */
@SpringBootApplication
public class FinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
