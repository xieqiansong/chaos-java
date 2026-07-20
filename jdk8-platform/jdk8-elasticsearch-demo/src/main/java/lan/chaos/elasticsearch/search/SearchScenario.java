package lan.chaos.elasticsearch.search;

import lan.chaos.elasticsearch.common.constant.EsConstants;
import lan.chaos.elasticsearch.common.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索场景 — 演示 match / term / range / bool 组合查询，以及排序与分页。
 *
 * <h3>text vs keyword 查询选择</h3>
 * <ul>
 *   <li>{@code matchQuery}：对 text 字段做全文检索，会先分词再匹配，适合商品名/描述</li>
 *   <li>{@code termQuery}：对 keyword 字段做精确匹配，不分词，适合分类/状态等枚举值</li>
 *   <li>{@code rangeQuery}：数值/日期范围，适合价格区间、时间区间</li>
 *   <li>{@code boolQuery}：must（与）/ should（或）/ filter（过滤不评分）/ mustNot（非）</li>
 * </ul>
 */
@Slf4j
@Service
public class SearchScenario {

    private final ElasticsearchRestTemplate restTemplate;

    public SearchScenario(ElasticsearchRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** 查询全部 */
    public List<Product> matchAll(int size) {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchAllQuery())
                .withPageable(PageRequest.of(0, size))
                .build();
        return search(query);
    }

    /** 全文检索：商品名或描述匹配关键词（text 字段） */
    public List<Product> matchByName(String keyword) {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword,
                        EsConstants.FIELD_NAME, EsConstants.FIELD_DESCRIPTION))
                .build();
        return search(query);
    }

    /** 精确匹配：分类（keyword 字段） */
    public List<Product> termByCategory(String category) {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.termQuery(EsConstants.FIELD_CATEGORY, category))
                .build();
        return search(query);
    }

    /** 范围查询：价格区间 [min, max] */
    public List<Product> rangeByPrice(Double min, Double max) {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.rangeQuery(EsConstants.FIELD_PRICE).gte(min).lte(max))
                .build();
        return search(query);
    }

    /** 复合查询：分类=指定值 且 (名称含关键词 或 描述含关键词)，并按价格降序分页 */
    public List<Product> boolQuery(String category, String keyword, int page, int size) {
        BoolQueryBuilder bool = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(EsConstants.FIELD_CATEGORY, category))
                .should(QueryBuilders.matchQuery(EsConstants.FIELD_NAME, keyword))
                .should(QueryBuilders.matchQuery(EsConstants.FIELD_DESCRIPTION, keyword))
                .minimumShouldMatch(1);
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(bool)
                .withSort(Sort.by(Sort.Direction.DESC, EsConstants.FIELD_PRICE))
                .withPageable(PageRequest.of(page, size))
                .build();
        return search(query);
    }

    private List<Product> search(NativeSearchQuery query) {
        SearchHits<Product> hits = restTemplate.search(query, Product.class);
        List<Product> list = hits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
        log.info("[search] 命中 {} 条", list.size());
        return list;
    }
}
