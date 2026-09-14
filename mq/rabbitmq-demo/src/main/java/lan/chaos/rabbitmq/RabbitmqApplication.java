package lan.chaos.rabbitmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RabbitMQ 演示模块启动类。
 *
 * <p>根包 {@code lan.chaos.rabbitmq}；技术点按能力分包（exchange / reliability / dlx），
 * 公共配置 / 常量 / 模型收进 {@code common/}。</p>
 *
 * <p>拓扑（Exchange / Queue / Binding）由 {@code common.config.RabbitConfig} 自动声明，
 * 连真实 Broker（docker-compose）即可直接跑，无需手动建组件。</p>
 */
@SpringBootApplication
public class RabbitmqApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitmqApplication.class, args);
    }
}
