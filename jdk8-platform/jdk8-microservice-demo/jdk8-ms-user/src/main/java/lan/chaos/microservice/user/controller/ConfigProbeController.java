package lan.chaos.microservice.user.controller;

import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.user.config.DynamicConfig;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P6 动态配置探针端点（演示/排障用，非业务接口）。
 *
 * <p>用途：在 Nacos 修改 {@code ms.config.greeting} 并发布后，不重启服务，多次 {@code GET /internal/config/greeting}
 * 即可看到文案变化，直观验证“配置热更新”。</p>
 *
 * <p>注意：本类本身也标了 {@code @RefreshScope}（与 {@link DynamicConfig} 一致），确保注入进来的
 * DynamicConfig 引用在刷新后指向重建后的新实例；若只给 DynamicConfig 标而这里不标，本 Controller 仍持有旧引用。</p>
 */
@RestController
@RequestMapping("/internal/config")
@RefreshScope
public class ConfigProbeController {

    private final DynamicConfig dynamicConfig;

    public ConfigProbeController(DynamicConfig dynamicConfig) {
        this.dynamicConfig = dynamicConfig;
    }

    @GetMapping("/greeting")
    public R<String> greeting() {
        return R.ok(dynamicConfig.getGreeting());
    }
}
