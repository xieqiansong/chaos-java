package lan.chaos.rocketmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RocketMQ 演示模块独立入口。
 * <p>
 * 只扫描 {@code lan.chaos.rocketmq} 包，按功能子包分层：
 * simple / order / delay / batch / transaction / filter / broadcast / retry / requestreply / pull 等。
 */
@SpringBootApplication(scanBasePackages = "lan.chaos.rocketmq")
public class RocketMqApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RocketMqApplication.class);
        app.run(args);
    }
}
