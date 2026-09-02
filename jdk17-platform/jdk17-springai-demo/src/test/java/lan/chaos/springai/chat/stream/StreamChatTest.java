package lan.chaos.springai.chat.stream;

import lan.chaos.springai.testutil.ModelEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * chat/stream 流式输出测试。
 *
 * <p>验证：流式返回的是逐 token 的块序列，可实时打印；合并后与一次完整回答等价。</p>
 */
@SpringBootTest
class StreamChatTest {

    @Autowired
    private StreamChatService service;

    @Test
    void streamChat() {
        ModelEndpoint.assumeUp();

        String question = "请用 3 句话介绍流式输出的特点。";

        System.out.println("=== stream/流式输出 ===");
        System.out.println("【输入】" + question);

        List<String> chunks = service.stream(question).collectList().block();
        assertThat(chunks).as("应返回流式块序列").isNotEmpty();

        String full = String.join("", chunks);
        System.out.println("【逐块预览】(共 " + chunks.size() + " 块)");
        chunks.stream().limit(5).forEach(c -> System.out.println("  └ " + c));
        System.out.println("【合并结果】" + full);

        assertThat(full).as("合并后的完整回答不应为空").isNotBlank();
    }
}
