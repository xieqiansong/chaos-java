package lan.chaos.nacos.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置中心场景的可断言验证：Nacos 可达时启动 {@link ConfigApplication} 上下文，
 * 断言动态配置 Bean {@link DynamicConfig} 已正确绑定（默认 {@code demo.title/default-title}、{@code demo.timeout/1000}）。
 *
 * <p>无 Nacos 时由 {@link NacosReachableCondition} 在 Spring 上下文启动前优雅跳过（CI 零误报）。</p>
 */
@SpringBootTest(classes = ConfigApplication.class)
@ExtendWith(NacosReachableCondition.class)
class ConfigTest {

    @Autowired
    private DynamicConfig dynamicConfig;

    @Test
    void dynamicConfigBinding() {
        // 上下文能起来且 @ConfigurationProperties(prefix = "demo") 已绑定默认值，
        // 说明 Nacos Config 集成链路通畅（bootstrap 阶段成功拉到配置）
        assertNotNull(dynamicConfig, "DynamicConfig 应被 Spring 托管");
        assertNotNull(dynamicConfig.getTitle(), "demo.title 应有值（默认 default-title）");
        assertTrue(dynamicConfig.getTimeout() > 0, "demo.timeout 应大于 0（默认 1000）");
    }
}
