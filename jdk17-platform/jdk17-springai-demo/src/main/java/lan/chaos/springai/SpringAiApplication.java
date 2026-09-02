package lan.chaos.springai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Spring AI 学习模块启动类。
 *
 * <p>本模块基于 Spring AI 1.1 + Spring Boot 3.5（JDK 17 平台），
 * 通过 OpenAI 兼容协议对接本地 llama.cpp llama-server（见 application.yml 的 base-url），
 * 覆盖对话/流式/记忆/工具调用/结构化输出/RAG 等高频场景。</p>
 *
 * <p>排除 {@link DataSourceAutoConfiguration}：pgvector starter 会把 spring-jdbc 带进 classpath，
 * 触发它无数据源配置时强行创建 Hikari 并报错。默认（内存向量库）本就不需要数据库；
 * 启用 pgvector profile 时由 {@code VectorStoreConfig} 显式提供 DataSource。</p>
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SpringAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiApplication.class, args);
    }
}
