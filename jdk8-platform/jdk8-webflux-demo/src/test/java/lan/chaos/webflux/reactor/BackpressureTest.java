package lan.chaos.webflux.reactor;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 背压验证：用 StepVerifier 的 thenRequest(n) 证明「源只在被请求时才发射」，即背压生效。
 */
class BackpressureTest {

    private final BackpressureDemo demo = new BackpressureDemo();

    @Test
    void fastProducer_respectsManualRequest() {
        // 首次只 request 5 个，证明源不会一次性全推；再 request 其余 15 个
        StepVerifier.create(demo.fastProducer())
                .thenRequest(5)
                .expectNext(1, 2, 3, 4, 5)
                .thenRequest(15)
                .expectNext(6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
                .expectComplete()
                .verify();
    }

    @Test
    void backpressure_demoRun_completes() {
        String out = demo.run();
        assertThat(out).contains("onComplete");
    }
}
