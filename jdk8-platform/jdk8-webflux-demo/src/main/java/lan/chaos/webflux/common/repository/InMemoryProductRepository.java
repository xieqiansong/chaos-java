package lan.chaos.webflux.common.repository;

import lan.chaos.webflux.common.model.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存仓储（演示支撑，零外部依赖）：预置 5 个样例商品，供 RouterFunction 与 Controller 共用。
 */
@Repository
public class InMemoryProductRepository {

    private final Map<Long, Product> store = new ConcurrentHashMap<>();

    public InMemoryProductRepository() {
        Product.samples(5).forEach(p -> store.put(p.getId(), p));
    }

    public List<Product> findAll() {
        return List.copyOf(store.values());
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Product save(Product product) {
        store.put(product.getId(), product);
        return product;
    }
}
