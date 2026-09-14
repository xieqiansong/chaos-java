package lan.chaos.springai.chat.stream;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * chat/stream：流式输出场景（逐 token 返回）。
 *
 * <p>WHY：对话类应用体验的关键——流式让用户"边想边看"，首 token 延迟远低于等待完整回答。
 * 与同步调用唯一的差异是 {@code call()} → {@code stream()}。</p>
 *
 * <p>本地适配（踩坑记录）：本机 Qwen3.5 是推理模型，llama-server 默认会先花大量 token 输出思考过程
 * （reasoning_content，Spring AI 不消费），导致 content 迟迟不出现甚至被思考占满。
 * 已实测确认 Spring AI 1.1.0 的 {@code OpenAiChatOptions.extraBody} 存在缺陷——定义了但
 * {@code OpenAiChatModel} 未消费（无法借此下发 {@code chat_template_kwargs} 关闭思考），
 * 因此推荐服务端启动时加 {@code --reasoning off} 一劳永逸。此处仅保留 maxTokens 作为输出长度上限。</p>
 */
@Service
public class StreamChatService {

    private final ChatClient chatClient;

    public StreamChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /** 流式对话：返回逐 token 的 {@link Flux}，可实时打印或推送给前端。 */
    public Flux<String> stream(String userMessage) {
        return chatClient.prompt(userMessage)
                .options(streamOptions())
                .stream()
                .content();
    }

    private ChatOptions streamOptions() {
        return OpenAiChatOptions.builder()
                .maxTokens(512)
                .build();
    }
}
