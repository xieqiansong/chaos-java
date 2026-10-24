package lan.chaos.oauth2.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第三方客户端启动类。启动时由 {@link flow.OAuth2FlowRunner} 程序化跑通整条 OAuth2 授权码链路。
 */
@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
}
