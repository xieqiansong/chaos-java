package lan.chaos.dubbo.consumer.it;

import lan.chaos.dubbo.provider.UserServiceImpl;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 注册中心集成测试配置：复用真实 {@link UserServiceImpl}（来自 dubbo-provider）作为 provider，
 * 挂到 Nacos 注册中心；{@link ItUserFacade} 通过注册中心发现并调用。
 *
 * <p>该配置只在 {@code ConsumerRegistryIT} 中加载，且由 {@link NacosReachableCondition}
 * 在 Nacos 不可达时整体跳过。</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableDubbo(scanBasePackages = "lan.chaos.dubbo.consumer.it")
@Import(UserServiceImpl.class)
public class RegistryITConfig {

    /** provider 导出端口（Dubbo 默认 20880，显式声明更稳妥）。 */
    @Bean
    public ProtocolConfig protocolConfig() {
        ProtocolConfig p = new ProtocolConfig();
        p.setName("dubbo");
        p.setPort(20880);
        return p;
    }
}
