package lan.chaos.elasticsearch.common.repository;

import lan.chaos.elasticsearch.common.model.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.util.List;

/**
 * 商品 Repository — 基于 Spring Data Elasticsearch 的声明式 CRUD。
 *
 * <p>继承 {@link ElasticsearchRepository} 即可获得 save / findById / findAll /
 * delete 等模板方法；方法名派生查询（如 {@code findByCategory}）由框架自动实现。</p>
 */
public interface ProductRepository extends ElasticsearchRepository<Product, String> {

    /** 按分类精确查询（keyword 字段） */
    List<Product> findByCategory(String category);
}
