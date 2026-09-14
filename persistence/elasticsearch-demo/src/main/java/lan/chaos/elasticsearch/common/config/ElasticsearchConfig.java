package lan.chaos.elasticsearch.common.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch 客户端配置。
 *
 * <p>显式构建 {@link RestHighLevelClient}，避免依赖 Spring Boot 自动配置对
 * {@code spring.elasticsearch.*} 属性前缀的版本差异；同时暴露
 * {@link ElasticsearchRestTemplate} 供搜索 / 聚合 / 索引管理场景使用。</p>
 */
@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchConfig {

    private final ElasticsearchProperties properties;

    public ElasticsearchConfig(ElasticsearchProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        List<HttpHost> hosts = Arrays.stream(properties.getUris().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(HttpHost::create)
                .collect(Collectors.toList());

        RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[0]))
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout((int) properties.getConnectTimeout())
                        .setSocketTimeout((int) properties.getSocketTimeout()));
        return new RestHighLevelClient(builder);
    }

    @Bean
    public ElasticsearchRestTemplate elasticsearchRestTemplate(RestHighLevelClient client) {
        return new ElasticsearchRestTemplate(client);
    }
}
