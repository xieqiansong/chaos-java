package lan.chaos.microservice.common.core.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一响应体 R 的契约测试：确保 code/data 语义稳定，前端依赖它做判断。
 */
class RTest {

    @Test
    void ok_shouldHaveCode0AndData() {
        R<String> r = R.ok("hello");
        assertEquals(0, r.getCode());
        assertEquals("hello", r.getData());
        assertTrue(r.getTimestamp() > 0);
    }

    @Test
    void okWithoutData_shouldHaveNullData() {
        R<Void> r = R.ok();
        assertEquals(0, r.getCode());
        assertNull(r.getData());
    }

    @Test
    void fail_shouldCarryGivenCodeAndMessage() {
        R<Void> r = R.fail(ResultCode.NOT_FOUND.getCode(), "没找到");
        assertEquals(404, r.getCode());
        assertEquals("没找到", r.getMessage());
    }
}
