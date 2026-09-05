package lan.chaos.mqtt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MQTT 演示模块启动类。
 *
 * <p>根包 {@code lan.chaos.mqtt}；技术点按能力分包（pubsub / qos / retained / lwt），
 * 公共常量 / 工具 / 模型收进 {@code common/}。</p>
 *
 * <p>本模块不依赖 Spring 的 MQTT 自动装配，直接用 Eclipse Paho 客户端连 Broker，
 * 因此 {@code @SpringBootApplication} 只负责组件扫描与 {@code @Service} 注入。</p>
 */
@SpringBootApplication
public class MqttApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqttApplication.class, args);
    }
}
