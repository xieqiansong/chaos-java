package lan.chaos.elasticsearch.document;

import lan.chaos.elasticsearch.common.repository.ProductRepository;
import lan.chaos.elasticsearch.common.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 文档场景 — 基于 {@link ProductRepository} 的 CRUD 与批量写入。
 *
 * <h3>关键认知</h3>
 * <ul>
 *   <li>Repository 写入是"近实时"的：默认写入后需 {@code refresh} 才能被搜索立即看到，
 *       演示中显式 {@code restTemplate.indexOps(Product.class).refresh()} 以便测试断言</li>
 *   <li>{@code save} 时若未指定 id，ES 自动生成；指定 id 则为 upsert</li>
 *   <li>{@code saveAll} 批量写入比逐条 {@code save} 高效，内部走 _bulk</li>
 * </ul>
 */
@Slf4j
@Service
public class DocumentScenario {

    private final ProductRepository repository;
    private final ElasticsearchRestTemplate restTemplate;

    public DocumentScenario(ProductRepository repository, ElasticsearchRestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    /** 保存单个文档，返回写入后的 id */
    public String save(Product product) {
        Product saved = repository.save(product);
        refresh();
        log.info("[document] 保存商品 id={} name={}", saved.getId(), saved.getName());
        return saved.getId();
    }

    /** 按 id 查询 */
    public Optional<Product> getById(String id) {
        return repository.findById(id);
    }

    /** 更新商品价格（先查后改，体现 upsert 语义） */
    public Product updatePrice(String id, Double newPrice) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + id));
        product.setPrice(newPrice);
        Product updated = repository.save(product);
        refresh();
        log.info("[document] 更新价格 id={} price={}", id, newPrice);
        return updated;
    }

    /** 删除文档 */
    public void delete(String id) {
        repository.deleteById(id);
        refresh();
        log.info("[document] 删除商品 id={}", id);
    }

    /** 批量写入，返回写入条数 */
    public int bulkSave(List<Product> products) {
        Iterable<Product> saved = repository.saveAll(products);
        int count = 0;
        for (Product ignored : saved) {
            count++;
        }
        refresh();
        log.info("[document] 批量写入 {} 条", count);
        return count;
    }

    /** 统计文档总数 */
    public long count() {
        return repository.count();
    }

    /** 刷新索引，使写入对搜索可见 */
    private void refresh() {
        restTemplate.indexOps(Product.class).refresh();
    }
}
