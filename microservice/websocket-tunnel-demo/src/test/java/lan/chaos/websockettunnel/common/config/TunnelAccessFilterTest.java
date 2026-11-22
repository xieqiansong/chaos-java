package lan.chaos.websockettunnel.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class TunnelAccessFilterTest {

    private static GatewayFilterChain recordingChain(AtomicBoolean chained) {
        return (ServerWebExchange exchange) -> {
            chained.set(true);
            return Mono.empty();
        };
    }

    @Test
    void rejectsForwardRequestWithoutToken() {
        TunnelProperties properties = new TunnelProperties();
        properties.setAccessToken("demo-token");
        TunnelAccessFilter filter = new TunnelAccessFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/forward/127.0.0.1/3306").build());
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, recordingChain(chained)).block();

        assertThat(chained).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void passesForwardRequestWithValidToken() {
        TunnelProperties properties = new TunnelProperties();
        properties.setAccessToken("demo-token");
        TunnelAccessFilter filter = new TunnelAccessFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/forward/127.0.0.1/3306?token=demo-token").build());
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, recordingChain(chained)).block();

        assertThat(chained).isTrue();
    }

    @Test
    void ignoresNonTunnelPaths() {
        TunnelProperties properties = new TunnelProperties();
        properties.setAccessToken("demo-token");
        TunnelAccessFilter filter = new TunnelAccessFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, recordingChain(chained)).block();

        assertThat(chained).isTrue();
    }
}
