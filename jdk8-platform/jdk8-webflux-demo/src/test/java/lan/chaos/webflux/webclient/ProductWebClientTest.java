package lan.chaos.webflux.webclient;

import lan.chaos.webflux.common.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebClient 验证：调本应用内嵌端点（RANDOM_PORT），证明非阻塞客户端可用。
 * 列表计数断言用「≥5」，因上下文内 InMemoryProductRepository 可能被其他测试追加数据。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductWebClientTest {

    @Autowired
    private ProductWebClient productWebClient;

    @Test
    void fetchProduct_returnsExisting() {
        StepVerifier.create(productWebClient.fetchProduct(1L))
                .assertNext(p -> assertThat(p.getId()).isEqualTo(1L))
                .verifyComplete();
    }

    @Test
    void fetchAll_returnsAtLeastSamples() {
        StepVerifier.create(productWebClient.fetchAll().count())
                .assertNext(c -> assertThat(c).isGreaterThanOrEqualTo(5L))
                .verifyComplete();
    }

    @Test
    void createAndFetch_persists() {
        Product created = productWebClient.createAndFetch(Product.sample(66L)).block();
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(66L);
    }
}
