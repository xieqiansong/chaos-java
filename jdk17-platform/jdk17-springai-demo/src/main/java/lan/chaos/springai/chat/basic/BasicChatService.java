package lan.chaos.springai.chat.basic;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * chat/basic：最简同步对话场景（一问一答）。
 *
 * <p>WHY：这是 Spring AI 最基础的能力。注入自动装配的 {@link ChatClient}，
 * 一条 {@code prompt(...).call().content()} 完成完整调用链——拼提示词、调模型、解析响应。</p>
 */
@Service
public class BasicChatService {

    private final ChatClient chatClient;

    public BasicChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /** 同步一问一答，返回完整回答。 */
    public String chat(String userMessage) {
        return chatClient.prompt(userMessage).call().content();
    }
}
