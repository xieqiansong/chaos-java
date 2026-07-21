package lan.chaos.jdk8features.base64;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class Base64DemoTest {

    @Test
    void encodeDecodeRoundTrip() {
        String raw = "JDK8 新特性：Base64 标准化";
        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);

        String encoded = Base64.getEncoder().encodeToString(bytes);
        assertArrayEquals(bytes, Base64.getDecoder().decode(encoded));

        String urlSafe = Base64.getUrlEncoder().encodeToString("a/b+c=".getBytes(StandardCharsets.UTF_8));
        assertFalse(urlSafe.contains("+") || urlSafe.contains("/"));
    }
}
