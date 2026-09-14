package lan.chaos.webflux.router;

import lan.chaos.webflux.common.model.Product;
import lan.chaos.webflux.common.repository.InMemoryProductRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * 处理函数：每个 HandlerFunction 接收 ServerRequest、返回 Mono&lt;ServerResponse&gt;（非阻塞）。
 */
@Component
public class ProductHandler {

    private final InMemoryProductRepository repository;

    public ProductHandler(InMemoryProductRepository repository) {
        this.repository = repository;
    }

    public Mono<ServerResponse> list(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(repository.findAll());
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        long id = Long.parseLong(request.pathVariable("id"));
        return Mono.justOrEmpty(repository.findById(id))
                .flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(Product.class)
                .map(repository::save)
                .flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p));
    }
}
