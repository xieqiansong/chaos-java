package lan.chaos.webflux.reactor;

import lan.chaos.webflux.common.model.Product;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reactor 基础验证：用 StepVerifier 断言 Mono/Flux 的语义（无需 Spring 上下文，纯库零外部依赖）。
 */
class MonoFluxBasicsTest {

    private final MonoFluxBasics basics = new MonoFluxBasics();

    @Test
    void fluxFromList_emitsAllSamples() {
        StepVerifier.create(basics.fluxFromList())
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    void mapPriceUp_multipliesPrice() {
        Product first = basics.mapPriceUp(basics.fluxFromList()).blockFirst();
        assertThat(first.getPrice()).isEqualTo(10.0 * 1.1);
    }

    @Test
    void flatMapAsyncEnrich_completesWithEnrichedStock() {
        List<Product> result = basics.flatMapAsyncEnrich(basics.fluxFromList())
                .collectList().block();
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getStock()).isGreaterThan(0);
    }

    @Test
    void filterInStock_keepsOnlyPositiveStock() {
        StepVerifier.create(basics.filterInStock(basics.fluxFromList()))
                .expectNextMatches(p -> p.getStock() > 0)
                .expectNextCount(4)
                .verifyComplete();
    }

    @Test
    void monoFromId_returnsSample() {
        assertThat(basics.monoFromId(3L).block().getName()).isEqualTo("product-3");
    }
}
