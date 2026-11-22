package lan.chaos.websockettunnel.common.config;

import lan.chaos.websockettunnel.tunnel.EmbeddedTunnelServer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用显式 {@code @Bean} 装配隧道服务，而不是让 Spring 去扫描隧道实现包。
 *
 * <p>这一点很关键：原项目入口类带 {@code @SpringBootApplication} 并无条件注册依赖
 * {@code TunnelLauncher} 的 ApplicationRunner，一旦被宿主扫到，没激活 server/client profile
 * 就会启动失败。这里只暴露一个纯粹的生命周期 Bean，与命令行/profile 彻底解耦。</p>
 */
@Configuration
@EnableConfigurationProperties(TunnelProperties.class)
public class EmbeddedTunnelConfiguration {

    @Bean
    public EmbeddedTunnelServer embeddedTunnelServer(TunnelProperties properties) {
        return new EmbeddedTunnelServer(properties);
    }
}
