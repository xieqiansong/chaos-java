package lan.chaos.microservice.order.client;

import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.order.model.UserDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserClientFallbackFactoryTest {

    @Test
    void fallback_returnsDegradedR() {
        // 无需 Spring 上下文 / 中间件：直接实例化工厂，触发降级分支
        UserClientFallbackFactory factory = new UserClientFallbackFactory();
        UserClient client = factory.create(new RuntimeException("downstream unavailable"));

        R<UserDTO> resp = client.getUser(1L);
        assertNotNull(resp);
        assertEquals(503, resp.getCode());   // SERVICE_DEGRADED
        assertNull(resp.getData());
    }
}
