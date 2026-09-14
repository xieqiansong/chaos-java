package lan.chaos.elasticsearch.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Elasticsearch 连接配置，绑定前缀 {@code app.elasticsearch}。
 *
 * <p>默认指向 localhost:9200；测试时由 Testcontainers 动态覆盖为容器地址。</p>
 */
@Data
@ConfigurationProperties(prefix = "app.elasticsearch")
public class ElasticsearchProperties {

    /** 逗号分隔的 ES 节点地址，如 http://localhost:9200 */
    private String uris = "http://localhost:9200";

    /** 连接超时（毫秒） */
    private long connectTimeout = 5000;

    /** Socket 超时（毫秒） */
    private long socketTimeout = 10000;
}
