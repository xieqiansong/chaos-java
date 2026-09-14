package lan.chaos.webflux.controller;

import lan.chaos.webflux.common.constant.ApiConstants;
import lan.chaos.webflux.common.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 注解式响应式 Controller 验证：与函数式路由对照，证明「同一引擎、两种写法」都可用。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void list_returnsProducts() {
        webTestClient.get().uri(ApiConstants.ANNOTATED_PRODUCTS)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Product.class)
                .value(list -> assertThat(list.size()).isGreaterThanOrEqualTo(5));
    }

    @Test
    void getById_returnsProduct() {
        webTestClient.get().uri(ApiConstants.ANNOTATED_PRODUCT_BY_ID, 2)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Product.class)
                .value(p -> assertThat(p.getId()).isEqualTo(2L));
    }

    @Test
    void create_persistsProduct() {
        Product created = webTestClient.post().uri(ApiConstants.ANNOTATED_PRODUCTS)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(Product.sample(88L))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Product.class)
                .returnResult().getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(88L);
    }
}
