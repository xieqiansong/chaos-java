package lan.chaos.testing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 单元测试专项 Demo 启动类。
 *
 * <p>本 demo 以「测试代码」本身作为核心交付物：
 * 所有 Mockito 和 Spring Boot 切片测试技巧都写在 test 目录下。
 * main 启动类仅用于确保项目可正常启动。
 */
@SpringBootApplication
public class TestingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestingApplication.class, args);
    }
}
