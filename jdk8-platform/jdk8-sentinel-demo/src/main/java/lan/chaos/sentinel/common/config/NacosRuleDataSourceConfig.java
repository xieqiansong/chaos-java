package lan.chaos.sentinel.common.config;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Sentinel 规则 Nacos 持久化（生产必选）。
 * <p>
 * 为什么需要：默认规则只存在于内存，应用重启即丢失；Dashboard 改的规则也不会回写。
 * 接 Nacos 后，规则持久化在配置中心——应用启动自动拉取、Dashboard 改完写回数据源、
 * 多实例共享同一份规则且重启不丢。这正是上一版 README「进阶方向」里未完成的那一步。
 * <p>
 * 用法：把 {@code application.yml} 中 {@code sentinel.nacos.enabled} 改为 {@code true}，填好 addr / namespace。
 * 本类通过 {@link ConditionalOnProperty} 默认关闭（{@code matchIfMissing=false}），
 * <b>因此不影响现有单元测试</b>——单测仍走代码初始化规则。
 * <p>
 * 实现选择：这里用程序化方式注册 {@link NacosDataSource}，而非 {@code spring.cloud.sentinel.datasource}
 * 的自动装配，好处是能精确控制 JSON 解析器与失败兜底——<b>即使 Nacos 暂不可达，应用仍能启动</b>，
 * 规则退化为代码初始化，仅打印告警，不会因连不上配置中心而启动失败。
 *
 * @author chaos
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "sentinel.nacos.enabled", havingValue = "true")
public class NacosRuleDataSourceConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sentinel.nacos.addr}")
    private String addr;
    @Value("${sentinel.nacos.namespace:}")
    private String namespace;
    @Value("${sentinel.nacos.username:}")
    private String username;
    @Value("${sentinel.nacos.password:}")
    private String password;
    @Value("${sentinel.nacos.group:SENTINEL_GROUP}")
    private String group;
    @Value("${sentinel.nacos.data-id.flow:}")
    private String flowDataId;
    @Value("${sentinel.nacos.data-id.degrade:}")
    private String degradeDataId;
    @Value("${sentinel.nacos.data-id.param-flow:}")
    private String paramFlowDataId;
    @Value("${sentinel.nacos.data-id.system:}")
    private String systemDataId;
    @Value("${sentinel.nacos.data-id.authority:}")
    private String authorityDataId;

    @PostConstruct
    public void init() {
        // Nacos 连接参数；namespace / 鉴权按需附加
        Properties props = new Properties();
        props.setProperty("serverAddr", addr);
        if (namespace != null && !namespace.isEmpty()) {
            props.setProperty("namespace", namespace);
        }
        if (username != null && !username.isEmpty()) {
            props.setProperty("username", username);
            props.setProperty("password", password);
        }

        register("flow", props, flowDataId,
                listParser(new TypeReference<List<FlowRule>>() {}),
                ds -> FlowRuleManager.register2Property(ds.getProperty()));
        register("degrade", props, degradeDataId,
                listParser(new TypeReference<List<DegradeRule>>() {}),
                ds -> DegradeRuleManager.register2Property(ds.getProperty()));
        register("param-flow", props, paramFlowDataId,
                listParser(new TypeReference<List<ParamFlowRule>>() {}),
                ds -> ParamFlowRuleManager.register2Property(ds.getProperty()));
        register("system", props, systemDataId,
                listParser(new TypeReference<List<SystemRule>>() {}),
                ds -> SystemRuleManager.register2Property(ds.getProperty()));
        register("authority", props, authorityDataId,
                listParser(new TypeReference<List<AuthorityRule>>() {}),
                ds -> AuthorityRuleManager.register2Property(ds.getProperty()));

        log.info("[Sentinel-Nacos] 规则数据源注册完成（addr={}, group={}）", addr, group);
    }

    /**
     * 注册单个规则类型的 Nacos 数据源。
     * <p>关键点：每个数据源独立 try-catch——某一类规则连不上（如 dataId 未建、Nacos 不可达）
     * 只告警、不阻断应用启动，其余规则类型照常生效。</p>
     */
    private <T> void register(String name, Properties props, String dataId,
                              Converter<String, List<T>> parser,
                              Consumer<ReadableDataSource<String, List<T>>> registrar) {
        if (dataId == null || dataId.isEmpty()) {
            log.warn("[Sentinel-Nacos] {} 规则 dataId 未配置，跳过", name);
            return;
        }
        try {
            ReadableDataSource<String, List<T>> source = new NacosRuleDataSource<>(props, group, dataId, parser);
            registrar.accept(source);
            log.info("[Sentinel-Nacos] {} 规则数据源已挂载: dataId={}", name, dataId);
        } catch (Exception e) {
            // Nacos 不可达 / dataId 为空配置时不应阻塞启动：规则退化为代码初始化
            log.warn("[Sentinel-Nacos] {} 规则数据源挂载失败（Nacos 可能不可达），规则回退为代码初始化: {}",
                    name, e.getMessage());
        }
    }

    /** Jackson 解析器：把 Nacos 中的 JSON 数组反序列化为对应规则列表 */
    private <T> Converter<String, List<T>> listParser(TypeReference<List<T>> typeRef) {
        return s -> {
            try {
                return objectMapper.readValue(s, typeRef);
            } catch (Exception e) {
                // Converter.convert 不声明受检异常，且解析失败需转运行时异常被外层捕获兜底
                throw new IllegalStateException("解析 Sentinel 规则失败: " + e.getMessage(), e);
            }
        };
    }
}
