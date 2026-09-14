package lan.chaos.nacos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 动态配置 Bean。
 *
 * <p>{@link RefreshScope} 是动态刷新的关键：当 Nacos 上对应 data-id 的配置发生变更时，
 * Spring Cloud 会销毁并重建该 Bean，使下次注入拿到最新值——无需重启应用。</p>
 *
 * <p>对应 Nacos 中的配置项（data-id 见 bootstrap.yml），例如：</p>
 * <pre>
 * demo:
 *   title: "hello nacos"
 *   timeout: 3000
 * </pre>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "demo")
public class DynamicConfig {

    /**
     * 业务标题，改 Nacos 后无需重启即可生效。
     */
    private String title = "default-title";

    /**
     * 超时时间（毫秒）。
     */
    private int timeout = 1000;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
