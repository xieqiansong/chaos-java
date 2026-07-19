package lan.chaos.nacos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 读取动态配置的接口，用于验证"改 Nacos 配置后无需重启即生效"。
 *
 * <p>演示两种读取方式：</p>
 * <ul>
 *     <li>注入 {@link DynamicConfig}（{@code @ConfigurationProperties} + {@code @RefreshScope}），推荐多字段场景</li>
 *     <li>{@code @Value} + 类上 {@link RefreshScope}，适合零散单值</li>
 * </ul>
 */
@RestController
@RefreshScope
public class ConfigController {

    private final DynamicConfig dynamicConfig;

    /**
     * 直接用 @Value 读取；配合类上的 @RefreshScope，Nacos 变更后同样会刷新。
     */
    @Value("${demo.title:default-title}")
    private String title;

    public ConfigController(DynamicConfig dynamicConfig) {
        this.dynamicConfig = dynamicConfig;
    }

    @GetMapping("/config")
    public Map<String, Object> read() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byProperties.title", dynamicConfig.getTitle());
        result.put("byProperties.timeout", dynamicConfig.getTimeout());
        result.put("byValue.title", title);
        return result;
    }
}
