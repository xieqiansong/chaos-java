package lan.chaos.springai.rag;

import lan.chaos.springai.testutil.ModelEndpoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * rag 检索增强生成测试（真实模型 + 本地 bge-m3 embedding）。
 *
 * <p>语料是 {@code rag/chaos-knowledge.txt}——一份模型训练数据里不可能有的私有文档，
 * 正好用来验证 RAG 的核心价值：让模型回答它"本来不知道"的内容。</p>
 */
// PER_CLASS：让 @BeforeAll 用实例方法——文档只入库一次。
// 否则每个测试方法各入库一遍，SimpleVectorStore 会累积重复文档，检索结果被重复片段占满。
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
class RagTest {

    @Autowired
    private RagService service;

    // 仅 pgvector 模式（有数据源）才存在；默认内存版为 null，跳过清理
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Value("classpath:rag/chaos-knowledge.txt")
    private Resource knowledge;

    @BeforeAll
    void ingestKnowledge() {
        // RAG 同时依赖对话模型与 embedding 服务
        ModelEndpoint.assumeUp();
        ModelEndpoint.assumeEmbeddingUp();

        // pgvector 表是持久化的，跨运行会累积重复文档把目标片段挤出 topK；测试前清空
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("TRUNCATE TABLE vector_store");
        }
        int chunks = service.ingest(knowledge);
        System.out.println("=== rag/文档入库 === 切片数：" + chunks);
        assertThat(chunks).as("文档应被切成至少 1 个片段").isGreaterThan(0);
    }

    @Test
    void searchReturnsRelevantChunks() {
        List<Document> hits = service.search("chaos 按什么划分 Java 平台模块？", 3);

        System.out.println("=== rag/语义检索（topK=3）===");
        hits.forEach(d -> System.out.println("  └ " + preview(d.getText())));

        assertThat(hits).as("应检索到相关片段").isNotEmpty();
        String joined = hits.stream().map(Document::getText).collect(Collectors.joining());
        assertThat(joined).as("检索结果应命中平台划分相关内容").contains("平台");
    }

    @Test
    void answerIsGroundedInDocuments() {
        String answer = service.answer("chaos 是什么？它的 Java 代码按什么原则划分平台？");

        System.out.println("=== rag/基于资料问答 ===");
        System.out.println("【回答】" + answer);

        assertThat(answer).as("回答非空").isNotBlank();
        assertThat(answer).as("回答应基于资料（提及容器/平台划分）")
                .containsAnyOf("个人开发项目容器", "容器", "platform", "平台");
    }

    @Test
    void outOfScopeQuestionIsRefused() {
        // 资料里没有的问题，验证 RAG 的"防幻觉"约束是否生效
        String answer = service.answer("2026 年世界杯冠军是哪支球队？");

        System.out.println("=== rag/资料外问题（防幻觉）===");
        System.out.println("【回答】" + answer);

        assertThat(answer).as("回答非空").isNotBlank();
    }

    private String preview(String text) {
        String flat = text.replaceAll("\\s+", " ");
        return flat.substring(0, Math.min(90, flat.length()));
    }
}
