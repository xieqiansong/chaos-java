package lan.chaos.elasticsearch.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * 商品文档实体 — 演示文档建模与字段映射。
 *
 * <p>注解说明：
 * <ul>
 *   <li>{@code @Document}：声明索引名与分片/副本策略</li>
 *   <li>{@code @Id}：映射到 ES 的 _id</li>
 *   <li>{@code @Field}：声明字段类型；text 用于全文检索，keyword 用于精确匹配/聚合，
 *       keyword 不参与分词，因此适合 term 查询与 terms 聚合</li>
 *   <li>{@code type = FieldType.Date}：日期字段，指定格式避免映射歧义</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "product", shards = 1, replicas = 0)
public class Product {

    @Id
    private String id;

    /** 商品名：text 类型，支持全文检索（默认标准分词器） */
    @Field(type = FieldType.Text)
    private String name;

    /** 分类：keyword 类型，用于精确匹配与聚合 */
    @Field(type = FieldType.Keyword)
    private String category;

    /** 价格：double，用于 range 查询与 avg 聚合 */
    @Field(type = FieldType.Double)
    private Double price;

    /** 库存：integer */
    @Field(type = FieldType.Integer)
    private Integer stock;

    /** 描述：text，参与全文检索 */
    @Field(type = FieldType.Text)
    private String description;

    /** 上架时间：date（使用 Spring Data ES 默认格式，避免 mapping 格式歧义） */
    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
}
