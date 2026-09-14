package lan.chaos.webflux;

import lan.chaos.webflux.reactor.BackpressureDemo;
import lan.chaos.webflux.reactor.MonoFluxBasics;
import lan.chaos.webflux.webclient.ProductWebClient;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 控制台 Runner：分节打印每个能力的「输入→输出」。
 * 想纯看效果不写测试时，直接跑这个 main 即可（Reactor 场景 + WebClient 调内嵌端点）。
 */
public class DemoApp {

    public static void main(String[] args) {
        // 启动 WebFlux 上下文（会拉起 Netty 服务器，供 WebClient 场景调用）
        try (ConfigurableApplicationContext ctx =
                     SpringApplication.run(WebFluxApplication.class, args)) {
            section("1. Reactor 响应式类型与操作符", ctx.getBean(MonoFluxBasics.class).run());
            section("2. 背压 Backpressure", ctx.getBean(BackpressureDemo.class).run());
            section("3. WebClient 异步非阻塞调用", ctx.getBean(ProductWebClient.class).run());
        }
    }

    private static void section(String title, String body) {
        System.out.println("\n========== " + title + " ==========");
        System.out.print(body);
    }
}
