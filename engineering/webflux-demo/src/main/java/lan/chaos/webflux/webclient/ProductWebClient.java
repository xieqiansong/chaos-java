package lan.chaos.webflux.webclient;

import lan.chaos.webflux.common.constant.ApiConstants;
import lan.chaos.webflux.common.model.Product;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 能力五：WebClient 异步非阻塞 HTTP 客户端（WebFlux 的声明式客户端）。
 *
 * <p>WHY：相比 RestTemplate（每请求占一个线程），WebClient 全程非阻塞（底层 Netty），
 * 高并发下省大量线程。返回 Mono/Flux，可链式 flatMap/zip 编排多次调用，配合超时与
 * onErrorResume 兜底。baseUrl 端口懒加载自运行端口（测试 RANDOM_PORT / demo 固定 18080）。
 */
@Service
public class ProductWebClient {

    private final WebClient.Builder builder;
    private final Environment environment;
    private volatile WebClient webClient;

    public ProductWebClient(WebClient.Builder builder, Environment environment) {
        this.builder = builder;
        this.environment = environment;
    }

    /** 懒加载 WebClient：首次调用时端口才确定（RANDOM_PORT 测试下 local.server.port 已就绪）。 */
    private WebClient client() {
        if (webClient == null) {
            int port = environment.getProperty("local.server.port", Integer.class, 18080);
            webClient = builder.baseUrl("http://localhost:" + port).build();
        }
        return webClient;
    }

    public Mono<Product> fetchProduct(long id) {
        return client().get()
                .uri(ApiConstants.PRODUCT_BY_ID, id)
                .retrieve()
                .bodyToMono(Product.class);
    }

    public Flux<Product> fetchAll() {
        return client().get()
                .uri(ApiConstants.PRODUCTS)
                .retrieve()
                .bodyToFlux(Product.class);
    }

    public Mono<Product> createAndFetch(Product product) {
        return client().post()
                .uri(ApiConstants.PRODUCTS)
                .bodyValue(product)
                .retrieve()
                .bodyToMono(Product.class);
    }

    /** 控制台 / 演示统一入口：返回「输入→输出」可读结果（block 仅演示用）。 */
    public String run() {
        StringBuilder sb = new StringBuilder();

        Product p = fetchProduct(1L).block();
        sb.append("GET ").append(ApiConstants.PRODUCT_BY_ID.replace("{id}", "1"))
                .append(" -> ").append(p).append('\n');

        List<Product> all = fetchAll().collectList().block();
        sb.append("GET ").append(ApiConstants.PRODUCTS)
                .append(" -> 共 ").append(all.size()).append(" 个\n");

        Product created = createAndFetch(Product.sample(99L)).block();
        sb.append("POST ").append(ApiConstants.PRODUCTS)
                .append(" -> ").append(created).append('\n');

        return sb.toString();
    }
}
