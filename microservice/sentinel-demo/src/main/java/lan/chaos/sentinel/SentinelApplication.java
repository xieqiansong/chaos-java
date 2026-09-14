package lan.chaos.sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sentinel 流量防卫兵 Demo 入口。
 *
 * <h3>两种运行方式</h3>
 * <ol>
 *   <li><b>单元测试（推荐）</b>：{@code mvn test}，无需任何外部依赖，规则由代码初始化</li>
 *   <li><b>Docker 完整体验</b>：{@code docker-compose up} 启动 Dashboard，再启动本应用，
 *       可在 Dashboard 界面实时查看/编辑限流规则</li>
 * </ol>
 *
 * <h3>核心学习点</h3>
 * <ul>
 *   <li>流控（Flow）：QPS 直接/关联/链路限流 + 快速失败 / WarmUp / 排队等待</li>
 *   <li>熔断降级（Degrade）：异常比例 / 异常数 / 慢调用比例自动熔断</li>
 *   <li>热点参数（Hotspot）：针对方法参数的精细化 QPS 控制</li>
 *   <li>@SentinelResource：注解式资源定义、blockHandler / fallback 降级处理</li>
 * </ul>
 *
 * @author chaos
 */
@SpringBootApplication
public class SentinelApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentinelApplication.class, args);
    }
}
