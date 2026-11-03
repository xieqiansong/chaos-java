package lan.chaos.dubbo.consumer.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import lan.chaos.dubbo.api.common.model.User;
import lan.chaos.dubbo.consumer.NacosReachableCondition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 注册中心发现链路验证（Nacos）。无 Docker / Nacos 时由 {@link NacosReachableCondition}
 * 优雅跳过，仅 {@code mvn verify} 阶段（failsafe）执行。
 *
 * <p>验证「provider 注册到 Nacos → consumer 从 Nacos 发现 → 跨进程调用」整条链路通畅。
 * 复用真实 {@link lan.chaos.dubbo.provider.UserServiceImpl}，与真实多进程形态一致。</p>
 */
@SpringBootTest(classes = RegistryITConfig.class)
@TestPropertySource(properties = {
        "dubbo.application.name=it-consumer",
        "dubbo.registry.address=nacos://127.0.0.1:8848"
})
@ExtendWith(NacosReachableCondition.class)
class ConsumerRegistryIT {

    @Autowired
    private ItUserFacade userFacade;

    @Test
    void discoveryViaNacos_thenInvoke() {
        User u = userFacade.getById(1L);
        assertNotNull(u);
        assertEquals(1L, u.getId());
    }
}
