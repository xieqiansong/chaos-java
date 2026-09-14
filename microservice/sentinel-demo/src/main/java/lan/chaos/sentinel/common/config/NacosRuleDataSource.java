package lan.chaos.sentinel.common.config;

import com.alibaba.csp.sentinel.datasource.AbstractDataSource;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 轻量 Nacos 规则数据源 —— 行为等价于官方 {@code sentinel-datasource-nacos} 的 {@code NacosDataSource}。
 * <p>
 * 为什么自己实现：本仓库为离线构建，官方适配包未被缓存；而 sentinel-core 自带的 datasource SPI
 * （{@link AbstractDataSource}/{@link Converter}）与 nacos-client 均已就绪，故在此用约 60 行复刻官方实现，
 * 避免引入无法下载的依赖。联网环境可直接换成官方依赖并删除本类。
 * <p>
 * 工作机制：构造时即拉取一次规则，随后注册 Nacos 监听器，配置变更时热更新；规则持久化在 Nacos 后重启不丢。
 *
 * @param <T> 规则类型（如 {@code List<FlowRule>}）
 */
public class NacosRuleDataSource<T> extends AbstractDataSource<String, T> {

    private final ConfigService configService;
    private final String groupId;
    private final String dataId;
    private final Listener listener;

    public NacosRuleDataSource(Properties properties, String groupId, String dataId,
                               Converter<String, T> parser) throws NacosException {
        super(parser);
        this.groupId = groupId;
        this.dataId = dataId;
        this.configService = NacosFactory.createConfigService(properties);

        // 配置变更回调：解析后热更新到 SentinelProperty
        this.listener = new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                if (configInfo == null || configInfo.isEmpty()) {
                    return;
                }
                try {
                    getProperty().updateValue(parser.convert(configInfo));
                } catch (Exception e) {
                    // 解析失败不应影响监听器后续工作，仅告警
                    logWarn("Nacos 规则解析失败 dataId=" + dataId, e);
                }
            }
        };

        // 启动即拉取一次（没有则保持默认，不阻断启动）
        try {
            String initial = configService.getConfig(dataId, groupId, 3000);
            if (initial != null && !initial.isEmpty()) {
                getProperty().updateValue(parser.convert(initial));
            }
        } catch (NacosException e) {
            logWarn("Nacos 初始拉取失败（稍后由监听器热更新）dataId=" + dataId, e);
        }

        configService.addListener(dataId, groupId, listener);
    }

    @Override
    public String readSource() throws Exception {
        return configService.getConfig(dataId, groupId, 3000);
    }

    @Override
    public void close() throws Exception {
        configService.removeListener(dataId, groupId, listener);
    }

    private static void logWarn(String msg, Throwable t) {
        org.slf4j.LoggerFactory.getLogger(NacosRuleDataSource.class).warn("[Sentinel-Nacos] " + msg, t);
    }
}
