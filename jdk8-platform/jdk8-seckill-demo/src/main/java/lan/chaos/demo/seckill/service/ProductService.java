package lan.chaos.demo.seckill.service;

import lan.chaos.demo.seckill.entity.Product;
import lan.chaos.demo.seckill.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 商品服务
 * <p>
 * 使用 Caffeine 本地缓存缓存商品信息（库存除外），降低 Redis/DB 压力。
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 查询商品（带本地缓存）
     */
    @Cacheable(value = "product", key = "#productId")
    public Optional<Product> getProduct(Long productId) {
        return productRepository.findById(productId);
    }

    /**
     * 创建商品
     */
    @CacheEvict(value = "product", key = "#product.id")
    public Product createProduct(Product product) {
        product.setStatus("DRAFT");
        return productRepository.save(product);
    }

    /**
     * 启动秒杀活动
     */
    @CacheEvict(value = "product", key = "#productId")
    public Product activateSeckill(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + productId));
        product.setStatus("ACTIVE");
        product.setStartTime(LocalDateTime.now());
        return productRepository.save(product);
    }

    /**
     * 更新商品状态
     */
    @CacheEvict(value = "product", key = "#productId")
    public void updateStatus(Long productId, String status) {
        productRepository.findById(productId).ifPresent(product -> {
            product.setStatus(status);
            productRepository.save(product);
        });
    }

    /**
     * 查询所有秒杀中的商品
     */
    public List<Product> getActiveProducts() {
        return productRepository.findByStatus("ACTIVE");
    }

    /**
     * 清除本地缓存
     */
    @CacheEvict(value = "product", key = "#productId")
    public void evictCache(Long productId) {
        // 清除缓存
    }
}
