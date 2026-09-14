package lan.chaos.springai.chat.basic;

import lan.chaos.springai.testutil.ModelEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * chat/basic 同步对话测试。
 *
 * <p>真实模型调用：无可用模型端点时经 {@link ModelEndpoint#assumeUp()} 优雅跳过。</p>
 */
@SpringBootTest
class BasicChatTest {

    @Autowired
    private BasicChatService service;

    @Test
    void syncChat() {
        ModelEndpoint.assumeUp();

        String question = "用一句话解释什么是 Java 的 Stream API。";

        System.out.println("=== basic/同步对话 ===");
        System.out.println("【输入】" + question);

        String answer = service.chat(question);

        System.out.println("【输出】" + answer);
        assertThat(answer).as("模型应返回非空回答").isNotBlank();
    }
}
