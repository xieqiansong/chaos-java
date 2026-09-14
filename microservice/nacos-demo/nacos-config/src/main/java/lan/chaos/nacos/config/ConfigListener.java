package lan.chaos.nacos.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * 编程式配置监听（场景 5）。
 *
 * <p>除 {@code @RefreshScope} 自动刷新外，还可通过 {@link com.alibaba.nacos.api.config.ConfigService#addListener}
 * 手动监听某个 data-id 的变更。适合需要在配置变更时执行自定义逻辑的场景，例如：</p>
 * <ul>
 *     <li>热更新线程池核心/最大线程数</li>
 *     <li>重新加载限流/路由规则</li>
 *     <li>刷新本地缓存、重建客户端连接</li>
 * </ul>
 *
 * <p>{@link NacosConfigManager} 由 spring-cloud-starter-alibaba-nacos-config 自动装配，
 * 通过它拿到底层 {@code ConfigService} 即可注册监听。</p>
 */
@Component
public class ConfigListener {

    private static final Logger log = LoggerFactory.getLogger(ConfigListener.class);

    private final NacosConfigManager nacosConfigManager;

    /**
     * 监听的 data-id，默认取应用名对应的配置文件；可在配置中覆盖。
     */
    @Value("${spring.application.name:nacos-config}.${spring.cloud.nacos.config.file-extension:yaml}")
    private String dataId;

    @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
    private String group;

    public ConfigListener(NacosConfigManager nacosConfigManager) {
        this.nacosConfigManager = nacosConfigManager;
    }

    /**
     * 应用启动完成后注册监听器；用 ApplicationReadyEvent 确保 Nacos 相关 Bean 已就绪。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerListener() throws NacosException {
        nacosConfigManager.getConfigService().addListener(dataId, group, new Listener() {
            @Override
            public Executor getExecutor() {
                // 返回 null 表示使用 Nacos 内置线程池执行回调
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("[ConfigListener] data-id={} group={} 配置发生变更，最新内容：\n{}", dataId, group, configInfo);
                // TODO 在此执行自定义热更新逻辑（重建线程池 / 重新加载规则等）
            }
        });
        log.info("[ConfigListener] 已注册监听：data-id={} group={}", dataId, group);
    }
}
