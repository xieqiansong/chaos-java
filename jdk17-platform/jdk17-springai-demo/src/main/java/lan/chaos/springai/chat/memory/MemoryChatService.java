package lan.chaos.springai.chat.memory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * chat/memory：多轮记忆场景（窗口式会话记忆）。
 *
 * <p>WHY：模型本身无状态。通过 {@link MessageChatMemoryAdvisor} 把历史消息自动注入每次请求，
 * 调用方只需按 {@code conversationId} 区分会话即可获得"多轮记忆"体验。</p>
 *
 * <p>记忆策略：{@link MessageWindowChatMemory} 固定保留最近 N 条消息（窗口滑动），
 * 实现简单、token 成本可控，是大多数场景的首选。</p>
 */
@Service
public class MemoryChatService {

    /** Advisor 从调用参数中取会话 ID 的 key（Spring AI 内部约定值，避免魔法字符串散落）。 */
    public static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    /** 窗口大小：每个会话保留最近 20 条消息。 */
    private static final int MAX_MESSAGES = 20;

    private final ChatClient memoryChatClient;

    public MemoryChatService(ChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(MAX_MESSAGES)
                .build();
        this.memoryChatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * 带记忆的对话：同一 {@code conversationId} 下自动携带历史上下文。
     *
     * @param conversationId 会话 ID（不同 ID 互不影响，可扩展为按用户/按会话隔离）
     */
    public String chat(String conversationId, String userMessage) {
        return memoryChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID_KEY, conversationId))
                .user(userMessage)
                .call()
                .content();
    }
}
