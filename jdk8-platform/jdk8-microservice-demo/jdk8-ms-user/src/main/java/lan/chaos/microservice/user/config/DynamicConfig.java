package lan.chaos.microservice.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * P6 动态配置演示 Bean。
 *
 * <p>WHY：把“会随环境/运营调整、且希望不重启生效”的配置做成 {@code @RefreshScope} Bean，
 * 由 {@code @ConfigurationProperties} 绑定到 {@code ms.config.*}。Nacos 配置中心里修改对应配置并发布后，
 * Spring Cloud 会收到 refresh 事件、销毁并重建这个 Bean，下一次读取就是新值——这就是“热更新”。
 *
 * <p>关键点：
 * <ul>
 *   <li>{@code @RefreshScope} 是 Spring Cloud 的作用域注解，本质是一个代理；配置变更时该 Bean 被销毁重建。</li>
 *   <li>只有 {@code @RefreshScope} 的 Bean 才会热更，普通 {@code @Component} 不会自动刷新（常见坑）。</li>
 *   <li>字段必须有 setter（{@code @ConfigurationProperties} 靠 setter 绑定），且前缀与 Nacos DataId 中的 key 对应。</li>
 * </ul>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "ms.config")
public class DynamicConfig {

    /** 演示用动态文案：本地默认在 application-common.yml，线上由 Nacos 的 ms-user.yaml 覆盖 */
    private String greeting = "你好（默认值）";

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }
}
