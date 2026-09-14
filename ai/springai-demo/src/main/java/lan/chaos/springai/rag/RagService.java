package lan.chaos.springai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * rag：检索增强生成（RAG）。
 *
 * <p>WHY：模型不知道你的私有知识（内部文档、业务数据），也无法靠训练记忆回答细节。
 * RAG 的思路是"先查资料，再作答"——把相关文档片段检索出来塞进提示词，让模型带资料回答，
 * 既降低幻觉，又能让知识随时更新（重建索引即可，不必重训模型）。</p>
 *
 * <p>完整链路：
 * <ol>
 *   <li><b>读取</b>：{@link TextReader} 把资源解析成 {@link Document}；</li>
 *   <li><b>切分</b>：{@link TokenTextSplitter} 按 token 窗口切片（长文档必须切，否则上下文装不下且检索精度差）；</li>
 *   <li><b>向量化入库</b>：{@link VectorStore#add} 内部调用 EmbeddingModel 把文本转成向量；</li>
 *   <li><b>语义检索</b>：{@link VectorStore#similaritySearch} 按向量相似度取回最相关片段；</li>
 *   <li><b>增强生成</b>：把片段作为参考资料注入提示词，模型据此作答。</li>
 * </ol></p>
 *
 * <p>存储层面向 {@link VectorStore} 抽象编程：具体实现由 {@code VectorStoreConfig} 决定——
 * 默认内存版（零依赖、测试友好，重启即丢），激活 {@code pgvector} profile 后切换为 PgVectorStore 持久化到 Postgres，
 * 业务代码一行不动。</p>
 */
@Service
public class RagService {

    private static final int DEFAULT_TOP_K = 3;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public RagService(VectorStore vectorStore, ChatClient.Builder builder) {
        this.vectorStore = vectorStore;
        this.chatClient = builder.build();
    }

    /**
     * 文档入库（ETL）：读取 → 切分 → 向量化 → 写入向量库。
     *
     * @return 切片数量
     */
    public int ingest(Resource resource) {
        TextReader reader = new TextReader(resource);
        reader.setCharset(StandardCharsets.UTF_8);
        List<Document> chunks = splitter().apply(reader.get());
        vectorStore.add(chunks);
        return chunks.size();
    }

    /** 语义检索：返回与问题最相关的文档片段。 */
    public List<Document> search(String query, int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build());
    }

    /** RAG 问答：基于检索到的资料作答，资料外的问题要求模型明确说"未提及"。 */
    public String answer(String question) {
        List<Document> relevant = search(question, DEFAULT_TOP_K);
        String context = relevant.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 用 Message 组装而非模板：文档内容可能含 {} 等字符，走模板渲染会被误解析为变量
        List<Message> messages = List.of(
                new SystemMessage("""
                        请严格依据下面的【参考资料】回答问题。
                        资料中没有的内容，请直接回答“资料中未提及”，不要编造。

                        【参考资料】
                        """ + context),
                new UserMessage(question));

        return chatClient.prompt(new Prompt(messages)).call().content();
    }

    private TokenTextSplitter splitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(200)
                .withMinChunkSizeChars(50)
                .build();
    }
}
