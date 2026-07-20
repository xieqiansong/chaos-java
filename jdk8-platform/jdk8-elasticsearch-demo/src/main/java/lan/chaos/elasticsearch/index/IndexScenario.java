package lan.chaos.elasticsearch.index;

import lan.chaos.elasticsearch.common.constant.EsConstants;
import lan.chaos.elasticsearch.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Service;

/**
 * 索引管理场景 — 演示索引的创建 / 存在性校验 / 删除。
 *
 * <h3>为什么索引管理很重要</h3>
 * <ul>
 *   <li>ES 是 schema-less 但有 mapping：字段类型一旦写入便难以修改，建索引前规划好映射</li>
 *   <li>生产环境通常用 {@code PUT /{index}} + settings + mappings 显式建索引，
 *       而非依赖首次写入自动建（auto-mapping 可能因首条数据误判类型）</li>
 *   <li>{@code IndexOperations} 同时支持基于实体类的"约定式"建索引与"自定义 mapping"建索引</li>
 * </ul>
 */
@Slf4j
@Service
public class IndexScenario {

    private final ElasticsearchRestTemplate restTemplate;

    public IndexScenario(ElasticsearchRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** 基于实体类 {@link Product} 的注解创建索引（含默认 mapping） */
    public boolean createIndexByEntity() {
        IndexOperations ops = restTemplate.indexOps(Product.class);
        if (ops.exists()) {
            ops.delete();
        }
        boolean created = ops.create();
        ops.putMapping(Product.class);
        log.info("[index] 创建索引 {} -> {}", EsConstants.INDEX_PRODUCT, created);
        return created;
    }

    /** 判断索引是否存在 */
    public boolean exists() {
        return restTemplate.indexOps(Product.class).exists();
    }

    /** 删除索引 */
    public boolean deleteIndex() {
        IndexOperations ops = restTemplate.indexOps(Product.class);
        if (!ops.exists()) {
            return true;
        }
        boolean deleted = ops.delete();
        log.info("[index] 删除索引 {} -> {}", EsConstants.INDEX_PRODUCT, deleted);
        return deleted;
    }
}
