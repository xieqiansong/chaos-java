package lan.chaos.dubbo.consumer.real;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 服务消费者启动类（角色单元，形态二）——真实多进程形态。
 *
 * <p>与 provider 协同：本应用通过 Nacos 发现 {@code dubbo-provider} 的实例，注入
 * {@link UserFacade} 这个「动态代理」，调用即跨进程。启动后 {@link ApplicationRunner}
 * 自动发起一次调用并打印「输入→输出」，验证整条链路通畅。</p>
 *
 * <p>运行前请先 {@code docker-compose up -d} 起 Nacos、再起 provider；本应用需带
 * {@code --spring.profiles.active=nacos} 以启用注册中心（见 application-nacos.yml）。</p>
 */
@EnableDubbo(scanBasePackages = "lan.chaos.dubbo.consumer.real")
@SpringBootApplication(scanBasePackages = "lan.chaos.dubbo.consumer.real")
public class ConsumerApplication implements ApplicationRunner {

    private final UserFacade userFacade;

    public ConsumerApplication(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("\n========== 真实多进程调用（Nacos 发现） ==========");
        System.out.println(userFacade.describeCall(1L));
    }
}
