package lan.chaos.microservice.common.log.mask;

import lan.chaos.microservice.common.log.annotation.Sensitive;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SensitiveMasker 单测：验证各类对象的脱敏行为（纯函数，无中间件）。
 */
class SensitiveMaskerTest {

    static class LoginForm {
        String username = "admin";
        @Sensitive
        String password = "s3cret";
        String note = "hello";
    }

    static class Nested {
        LoginForm form = new LoginForm();
        String tag = "x";
    }

    @Test
    void shouldMaskSensitiveMapKeys() {
        Map<String, Object> m = new HashMap<>();
        m.put("username", "admin");
        m.put("password", "s3cret");
        m.put("token", "jwt-xxx");

        String out = SensitiveMasker.mask(m);

        assertTrue(out.contains("username=admin"), "非敏感字段原样保留");
        assertTrue(out.contains("password=******"), "password 被打码");
        assertTrue(out.contains("token=******"), "token 被打码");
        assertFalse(out.contains("s3cret"), "明文密码不得出现");
        assertFalse(out.contains("jwt-xxx"), "明文 token 不得出现");
    }

    @Test
    void shouldMaskAnnotatedField() {
        String out = SensitiveMasker.mask(new LoginForm());
        assertTrue(out.contains("username=admin"));
        assertTrue(out.contains("password=******"), "@Sensitive 标注字段被打码");
        assertTrue(out.contains("note=hello"));
        assertFalse(out.contains("s3cret"));
    }

    @Test
    void shouldMaskNestedObject() {
        String out = SensitiveMasker.mask(new Nested());
        assertTrue(out.contains("tag=x"));
        assertTrue(out.contains("password=******"), "嵌套对象内的敏感字段也打码");
        assertFalse(out.contains("s3cret"));
    }

    @Test
    void shouldMaskCollection() {
        String out = SensitiveMasker.mask(Arrays.asList(new LoginForm(), "plain"));
        assertTrue(out.contains("password=******"));
        assertTrue(out.contains("plain"));
    }

    @Test
    void shouldKeepPlainStringAndNumber() {
        assertTrue(SensitiveMasker.mask("just-a-string").equals("just-a-string"));
        assertTrue(SensitiveMasker.mask(123).equals("123"));
        assertTrue(SensitiveMasker.mask(null).equals("null"));
    }

    @Test
    void shouldTruncateOversizedString() {
        String big = "a".repeat(500);
        String out = SensitiveMasker.mask(big);
        assertTrue(out.endsWith("(truncated)"), "超长字符串被截断");
        assertFalse(out.contains("a".repeat(500)));
    }
}
