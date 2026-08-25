package lan.chaos.ratelimiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 限流 demo 启动类。
 *
 * <p>两种运行方式：
 * <ul>
 *   <li>默认：以 Web 方式启动，暴露 REST 限流演示接口（见 RateLimiterController）。</li>
 *   <li>压测：启动时追加 --ratelimiter.bench.enabled=true 及 bench 参数，跑完打印后退出（见 BenchRunner）。</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RatelimiterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RatelimiterApplication.class, args);
    }
}