package lan.chaos.webflux.reactor;

import lan.chaos.webflux.common.model.Product;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * 能力一：Reactor 响应式类型与操作符（WebFlux 的引擎）。
 *
 * <p>WHY：Mono = 0|1 个元素，Flux = 0..N 个元素，二者都是「声明式数据流」——
 * 直到订阅(subscribe)才真正执行，且天然非阻塞、内建背压。与 CompletableFuture 最大区别是
 * 内建背压与丰富操作符(map/flatMap/filter/zip)，不用手写回调嵌套。
 * 关键 API：just/justOrEmpty（创建）、map（同步转换）、flatMap（异步展开为 Mono/Flux）、
 * filter（过滤）、collectList（汇聚）、block（仅测试/演示用，生产禁止阻塞）。
 */
@Service
public class MonoFluxBasics {

    /** 从样例集合创建 Flux（0..N）。 */
    public Flux<Product> fluxFromList() {
        return Flux.fromIterable(Product.samples(5));
    }

    /** map：同步转换每个元素（此处把价格 ×1.1）。 */
    public Flux<Product> mapPriceUp(Flux<Product> source) {
        return source.map(p -> {
            p.setPrice(p.getPrice() * 1.1);
            return p;
        });
    }

    /**
     * flatMap：把每个元素异步展开成新 Mono/Flux（典型用于切换线程池 / 远程调用）。
     * 这里用 delayElement 模拟「异步补全库存/价格」的远程调用，再合并结果。
     */
    public Flux<Product> flatMapAsyncEnrich(Flux<Product> source) {
        return source.flatMap(p ->
                Mono.just(p)
                        .delayElement(Duration.ofMillis(10))
                        .map(x -> {
                            x.setStock(x.getStock() + 100);
                            return x;
                        }));
    }

    /** filter：只保留库存为正的商品。 */
    public Flux<Product> filterInStock(Flux<Product> source) {
        return source.filter(p -> p.getStock() > 0);
    }

    /** Mono：0|1 个元素（按 id 取一个样例商品）。 */
    public Mono<Product> monoFromId(long id) {
        return Mono.justOrEmpty(Product.sample(id));
    }

    /** 控制台 / 测试统一入口：返回「输入→输出」可读结果。 */
    public String run() {
        StringBuilder sb = new StringBuilder();

        List<Product> list = fluxFromList().collectList().block();
        sb.append("fluxFromList()            -> ").append(list.size()).append(" 个商品\n");

        Product firstMapped = mapPriceUp(fluxFromList()).blockFirst();
        sb.append("map(price*1.1) 首件      -> ").append(firstMapped).append("\n");

        List<Product> enriched = flatMapAsyncEnrich(fluxFromList()).collectList().block();
        sb.append("flatMap 异步补全库存 末件 -> ").append(enriched.get(enriched.size() - 1)).append("\n");

        long inStock = filterInStock(fluxFromList()).count().block();
        sb.append("filter(stock>0) 命中数    -> ").append(inStock).append("\n");

        Product byId = monoFromId(3L).block();
        sb.append("monoFromId(3)             -> ").append(byId).append("\n");

        return sb.toString();
    }
}
