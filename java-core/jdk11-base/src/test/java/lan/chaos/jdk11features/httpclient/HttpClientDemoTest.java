package lan.chaos.jdk11features.httpclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpClientDemoTest {

    @Test
    void localPingRoundTrip() throws Exception {
        // 用 JDK 内置 HttpServer 做真实收发，无需外网
        assertEquals("pong", HttpClientDemo.localPing());
    }
}
