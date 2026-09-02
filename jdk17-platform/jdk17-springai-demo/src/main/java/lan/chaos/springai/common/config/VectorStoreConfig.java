package lan.chaos.springai.common.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * 向量库配置：决定 RAG 用哪种存储实现。
 *
 * <p>WHY：业务代码只依赖 {@link VectorStore} 接口，具体实现交给配置——
 * 这正是"面向接口编程"在 AI 工程里的收益：换存储不用改一行业务代码。</p>
 *
 * <ul>
 *   <li>默认：内存版 {@link SimpleVectorStore}，零依赖、测试友好，重启即丢；</li>
 *   <li>{@code pgvector} profile：由 Spring AI 自动配置提供 PgVectorStore（持久化到 Postgres），
 *       数据源在此处显式提供（因为主类已排除 DataSourceAutoConfiguration）。</li>
 * </ul>
 */
@Configuration
public class VectorStoreConfig {

    /**
     * 默认内存向量库：仅在<b>未</b>启用 pgvector profile 时生效。
     * 启用 pgvector 后由自动配置提供 PgVectorStore，两者通过 profile 互斥，不会产生 Bean 冲突。
     */
    @Bean
    @Profile("!pgvector")
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * pgvector profile 下的数据源属性：绑定 application-pgvector.yml 的 spring.datasource.*。
     * 主类已全局排除 DataSourceAutoConfiguration，故此处显式提供属性与数据源。
     */
    @Bean
    @Profile("pgvector")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties pgVectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * pgvector profile 下的数据源：用 {@link DataSourceProperties#initializeDataSourceBuilder()} 构建，
     * 它会把 spring.datasource.url 正确翻译成 Hikari 的 jdbcUrl（裸 DataSourceBuilder 不会做这层翻译）。
     */
    @Bean
    @Profile("pgvector")
    public DataSource pgVectorDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
