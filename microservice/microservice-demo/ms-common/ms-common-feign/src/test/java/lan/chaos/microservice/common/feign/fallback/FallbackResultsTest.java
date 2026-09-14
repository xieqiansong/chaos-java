package lan.chaos.microservice.common.feign.fallback;

import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.common.core.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FallbackResultsTest {

    @Test
    void degraded_returnsServiceDegradedCode() {
        R<Object> r = FallbackResults.degraded();
        assertEquals(ResultCode.SERVICE_DEGRADED.getCode(), r.getCode());
        assertNotNull(r.getMessage());
    }

    @Test
    void degraded_withDetail_appendsDetail() {
        R<Object> r = FallbackResults.degraded("ms-user#getUser(1)");
        assertEquals(ResultCode.SERVICE_DEGRADED.getCode(), r.getCode());
        assertTrue(r.getMessage().contains("ms-user#getUser(1)"));
    }
}
