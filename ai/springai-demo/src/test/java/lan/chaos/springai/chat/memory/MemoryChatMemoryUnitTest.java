package lan.chaos.springai.chat.memory;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * chat/memory 窗口记忆纯单元测试（不依赖模型/外部服务）。
 *
 * <p>验证 {@link MessageWindowChatMemory} 的窗口滑动语义：只保留最近 N 条消息。</p>
 */
class MemoryChatMemoryUnitTest {

    @Test
    void windowChatMemoryKeepsOnlyRecentMessages() {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(4)
                .build();
        String conversationId = "unit-conv-1";

        for (int i = 1; i <= 6; i++) {
            memory.add(conversationId, new UserMessage("第 " + i + " 条消息"));
        }

        List<Message> history = memory.get(conversationId);
        assertThat(history).as("窗口只保留最近 4 条").hasSize(4);
        assertThat(history.get(0).getText()).as("最旧的第 1、2 条应被滑出").isEqualTo("第 3 条消息");
        assertThat(history.get(3).getText()).isEqualTo("第 6 条消息");

        System.out.println("窗口截断后剩余消息：" + history.stream().map(Message::getText).toList());
    }
}
