package lan.chaos.webflux.router;

import lan.chaos.webflux.common.constant.ApiConstants;
import lan.chaos.webflux.common.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 函数式端点验证：WebTestClient 直连内嵌 Netty 服务器（RANDOM_PORT），零外部依赖。
 * 因 InMemoryProductRepository 在上下文内被多测试共享，列表断言用「≥5」避免顺序耦合。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductRouterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void list_returnsProducts() {
        webTestClient.get().uri(ApiConstants.PRODUCTS)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Product.class)
                .value(list -> assertThat(list.size()).isGreaterThanOrEqualTo(5));
    }

    @Test
    void getById_returnsProduct() {
        webTestClient.get().uri(ApiConstants.PRODUCT_BY_ID, 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Product.class)
                .value(p -> assertThat(p.getId()).isEqualTo(1L));
    }

    @Test
    void getById_notFound_whenMissing() {
        webTestClient.get().uri(ApiConstants.PRODUCT_BY_ID, 9999)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void create_persistsProduct() {
        Product created = webTestClient.post().uri(ApiConstants.PRODUCTS)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(Product.sample(77L))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Product.class)
                .returnResult().getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(77L);
    }
}
