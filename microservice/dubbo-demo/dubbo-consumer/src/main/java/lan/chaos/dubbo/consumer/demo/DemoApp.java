package lan.chaos.dubbo.consumer.demo;

import lan.chaos.dubbo.consumer.demo.facade.AsyncUserFacade;
import lan.chaos.dubbo.consumer.demo.facade.BasicUserFacade;
import lan.chaos.dubbo.consumer.demo.facade.ClusterFaultTolerantFacade;
import lan.chaos.dubbo.consumer.demo.facade.LoadBalanceFacade;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 控制台 Runner：分节打印 Dubbo 各机制的「输入→输出」，零外部依赖、纯本地直连即可看效果。
 *
 * <p>直接运行本 main（或 IDE 里执行），会以「单进程点对点」形态启动 DemoApplication，
 * 依次演示：基础同步调用 / 异步调用 / 集群容错（failover 重试）/ 负载均衡（roundrobin）。</p>
 */
public class DemoApp {

    public static void main(String[] args) throws Exception {
        try (ConfigurableApplicationContext ctx =
                     SpringApplication.run(DemoApplication.class, args)) {
            section("1. 基础同步调用 @DubboReference（直连 20880 / group=basic）",
                    ctx.getBean(BasicUserFacade.class).describe(1L));
            section("2. 异步调用 CompletableFuture（直连 20880 / group=async）",
                    ctx.getBean(AsyncUserFacade.class).describe(1L));
            section("3. 集群容错 failover/retries=2（直连 20880 / group=fault）",
                    ctx.getBean(ClusterFaultTolerantFacade.class).describe(1L));
            section("4. 负载均衡 roundrobin（直连 20880 / group=lbA + lbB）",
                    ctx.getBean(LoadBalanceFacade.class).describe(10));
        }
    }

    private static void section(String title, String body) {
        System.out.println("\n========== " + title + " ==========");
        System.out.println(body);
    }
}
