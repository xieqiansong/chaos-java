package lan.chaos.nacos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 配置中心演示启动类。
 *
 * <p>Nacos Config 在 bootstrap 阶段（应用上下文之前）读取远程配置，
 * 因此相关连接信息必须写在 {@code bootstrap.yml} 而非 {@code application.yml}。</p>
 */
@SpringBootApplication
public class ConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class, args);
    }
}
