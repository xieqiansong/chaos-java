package lan.chaos.dubbo.consumer.demo;

import org.apache.dubbo.config.ProtocolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 演示用协议配置：单个 dubbo 协议、端口 20880。
 *
 * <p>演示的五个 provider 各有不同 {@code group}（basic/async/fault/lbA/lbB），靠 group 区分同一端口上的不同服务，
 * 因此无需为每个 provider 分配独立端口。consumer 端用 {@code url + group} 精确直连对应服务。</p>
 *
 * <p>注意：此处没有配置任何 {@code RegistryConfig}，是无注册中心的「点对点直连」，
 * 与真实 {@code application-nacos.yml} 挂 Nacos 的形态形成对照。</p>
 */
@Configuration
public class DemoConfig {

    @Bean
    public ProtocolConfig demoProtocol() {
        ProtocolConfig p = new ProtocolConfig();
        p.setName("dubbo");
        p.setPort(20880);
        return p;
    }
}
