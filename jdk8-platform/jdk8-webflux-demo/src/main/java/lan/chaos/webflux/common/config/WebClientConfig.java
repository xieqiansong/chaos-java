package lan.chaos.webflux.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient 构建配置。
 *
 * <p>WHY：WebClient 默认没有整体响应超时——「非阻塞」不等于「无限等待」，下游长时间不返回会一直
 * 占用连接。这里统一设 responseTimeout，超时后 Mono 以超时错误终结，调用方用 onErrorResume 兜底，
 * 链路不会卡死。这也是生产接入 WebClient 的首要坑点。
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(3));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
