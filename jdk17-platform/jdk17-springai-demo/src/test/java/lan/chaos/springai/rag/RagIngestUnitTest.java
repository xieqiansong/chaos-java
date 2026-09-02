package lan.chaos.springai.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * rag 纯单元测试：ETL 的"读取 + 切分"环节（不依赖模型与 embedding 服务）。
 *
 * <p>WHY：切片质量直接决定检索效果——切太碎丢语义，切太大检索不准。
 * 这一层是纯本地逻辑，值得单独单测固化。</p>
 */
class RagIngestUnitTest {

    private final ClassPathResource knowledge = new ClassPathResource("rag/chaos-knowledge.txt");

    @Test
    void textReaderLoadsDocument() {
        TextReader reader = new TextReader(knowledge);
        reader.setCharset(StandardCharsets.UTF_8);

        List<Document> documents = reader.get();

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getText()).contains("chaos");
    }

    @Test
    void tokenTextSplitterSplitsIntoChunks() {
        TextReader reader = new TextReader(knowledge);
        reader.setCharset(StandardCharsets.UTF_8);
        List<Document> documents = reader.get();

        List<Document> chunks = TokenTextSplitter.builder()
                .withChunkSize(120)
                .withMinChunkSizeChars(20)
                .build()
                .apply(documents);

        System.out.println("CHUNKS=" + chunks.size());
        chunks.forEach(c -> System.out.println("  └ " + c.getText().replaceAll("\\s+", " ")));

        assertThat(chunks.size()).as("文档应被切成多个片段").isGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> assertThat(c.getText()).isNotBlank());

        // 切片必须是原文的连续片段：切分若产生拼接/重排，检索会拿到错乱的上下文
        String original = documents.get(0).getText();
        chunks.forEach(c -> assertThat(original)
                .as("切片应是原文的连续片段，实际为：" + c.getText())
                .contains(c.getText().trim()));
    }
}
