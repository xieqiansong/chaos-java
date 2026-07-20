package lan.chaos.elasticsearch.aggregation;

import lan.chaos.elasticsearch.common.constant.EsConstants;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聚合场景 — 演示 terms 分桶统计与 avg 平均值聚合。
 *
 * <h3>聚合（Aggregation）是什么</h3>
 * <p>类似 SQL 的 {@code GROUP BY}：在不返回明细的情况下，对数据进行分组统计。
 * 典型场景：每个分类下有多少商品、平均价格多少、价格分布直方图等。</p>
 *
 * <h3>关键认知</h3>
 * <ul>
 *   <li>terms 聚合只能作用在 <b>keyword</b> 字段上（text 字段需加 .keyword 子字段）</li>
 *   <li>聚合与搜索共用一个查询：可以用 query 先过滤，再对过滤后的结果聚合</li>
 *   <li>聚合结果不返回文档明细，因此 size 可设为 0 以节省开销</li>
 * </ul>
 */
@Slf4j
@Service
public class AggregationScenario {

    private final ElasticsearchRestTemplate restTemplate;

    public AggregationScenario(ElasticsearchRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** 按分类统计商品数量（terms 聚合） */
    public Map<String, Long> countByCategory() {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(org.elasticsearch.index.query.QueryBuilders.matchAllQuery())
                .withTrackTotalHits(true)
                .addAggregation(AggregationBuilders.terms("byCategory")
                        .field(EsConstants.FIELD_CATEGORY)
                        .size(20))
                .build();
        // size=0 表示不返回明细文档，只看聚合结果
        query.setMaxResults(0);

        SearchHits<?> hits = restTemplate.search(query, Map.class);
        AggregationsContainer<Aggregations> container =
                (AggregationsContainer<Aggregations>) hits.getAggregations();
        Aggregations aggregations = container.aggregations();
        Map<String, Long> result = new LinkedHashMap<>();
        if (aggregations != null) {
            Terms terms = aggregations.get("byCategory");
            terms.getBuckets().forEach(b -> result.put(b.getKeyAsString(), b.getDocCount()));
        }
        log.info("[aggregation] 各分类商品数: {}", result);
        return result;
    }

    /** 按分类统计平均价格（terms 嵌套 avg 子聚合） */
    public Map<String, Double> avgPriceByCategory() {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(org.elasticsearch.index.query.QueryBuilders.matchAllQuery())
                .addAggregation(AggregationBuilders.terms("byCategory")
                        .field(EsConstants.FIELD_CATEGORY)
                        .size(20)
                        .subAggregation(AggregationBuilders.avg("avgPrice")
                                .field(EsConstants.FIELD_PRICE)))
                .build();
        query.setMaxResults(0);

        SearchHits<?> hits = restTemplate.search(query, Map.class);
        AggregationsContainer<Aggregations> container =
                (AggregationsContainer<Aggregations>) hits.getAggregations();
        Aggregations aggregations = container.aggregations();
        Map<String, Double> result = new LinkedHashMap<>();
        if (aggregations != null) {
            Terms terms = aggregations.get("byCategory");
            terms.getBuckets().forEach(b -> {
                Avg avg = b.getAggregations().get("avgPrice");
                result.put(b.getKeyAsString(), avg.getValue());
            });
        }
        log.info("[aggregation] 各分类平均价格: {}", result);
        return result;
    }
}
