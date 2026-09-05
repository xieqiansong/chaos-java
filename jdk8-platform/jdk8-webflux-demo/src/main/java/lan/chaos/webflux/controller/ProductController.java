package lan.chaos.webflux.controller;

import lan.chaos.webflux.common.constant.ApiConstants;
import lan.chaos.webflux.common.model.Product;
import lan.chaos.webflux.common.repository.InMemoryProductRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 能力四（对照）：注解式响应式 Controller。
 *
 * <p>WHY：@RestController + Mono/Flux 返回值，是「从 Spring MVC 平滑迁移」的形态——写法与 MVC 几乎一致，
 * 只是方法返回 Mono/Flux 即非阻塞。适合端点多、团队熟悉 MVC 的项目。与 RouterFunction 二选一；
 * 两者底层都是同一套 WebFlux 引擎，路径不冲突即可共存（此处用 /api/annotated 前缀区分）。
 */
@RestController
@RequestMapping(value = ApiConstants.ANNOTATED_PRODUCTS, produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductController {

    private final InMemoryProductRepository repository;

    public ProductController(InMemoryProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Flux<Product> list() {
        return Flux.fromIterable(repository.findAll());
    }

    @GetMapping("/{id}")
    public Mono<Product> getById(@PathVariable Long id) {
        return Mono.justOrEmpty(repository.findById(id));
    }

    @PostMapping
    public Mono<Product> create(@RequestBody Product product) {
        return Mono.just(repository.save(product));
    }
}
