package lan.chaos.microservice.common.log.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 访问日志开关与采样配置（{@code logging.access.*}）。
 *
 * <p>默认开启。生产环境若某些接口 QPS 极高、日志量太大，可整体关掉或关闭入参打印（{@code include-args=false}）。</p>
 */
@ConfigurationProperties(prefix = "logging.access")
public class AccessLogProperties {

    /** 是否开启访问日志切面，默认 true */
    private boolean enabled = true;

    /** 是否在日志里打印入参（脱敏后），默认 true；关闭后只记录 方法/耗时/成败 */
    private boolean includeArgs = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeArgs() {
        return includeArgs;
    }

    public void setIncludeArgs(boolean includeArgs) {
        this.includeArgs = includeArgs;
    }
}
