package lan.chaos.elasticsearch.common.constant;

/**
 * Elasticsearch 公共常量。
 *
 * <p>集中管理索引名、字段名、分片/副本数，避免散落的魔法值。</p>
 */
public final class EsConstants {

    private EsConstants() {
    }

    /** 商品索引名 */
    public static final String INDEX_PRODUCT = "product";

    /** 默认分片数 / 副本数（单机演示够用） */
    public static final int SHARD_NUM = 1;
    public static final int REPLICA_NUM = 0;

    /** 文档字段名（与 {@code Product} 实体字段一一对应） */
    public static final String FIELD_NAME = "name";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_PRICE = "price";
    public static final String FIELD_STOCK = "stock";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CREATED_AT = "createdAt";
}
