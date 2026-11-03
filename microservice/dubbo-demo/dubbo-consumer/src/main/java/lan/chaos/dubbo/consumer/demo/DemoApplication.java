package lan.chaos.dubbo.consumer.demo;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 单进程「点对点」演示启动类（无注册中心）。
 *
 * <p>本包把 provider 与 consumer 放进同一个 JVM：provider 用 {@code @DubboService} 导出到
 * 本地端口、consumer 用 {@code @DubboReference(url=...)} 直连，全程不依赖 Nacos。
 * 目的是用最少噪音、零外部依赖，把 Dubbo「服务暴露 / 引用 / 异步 / 集群容错 / 负载均衡」
 * 各机制逐一跑通并可观察。</p>
 *
 * <p>真实多进程形态请见 {@code lan.chaos.dubbo.consumer.real} 包 + Nacos 注册中心。</p>
 */
@EnableDubbo(scanBasePackages = "lan.chaos.dubbo.consumer.demo")
@SpringBootApplication(scanBasePackages = "lan.chaos.dubbo.consumer.demo")
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
