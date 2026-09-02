package lan.chaos.springai;

import lan.chaos.springai.testutil.ModelEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 骨架冒烟测试：验证「Spring AI ↔ 本地 llama-server(OpenAI 兼容)」链路真实可用。
 *
 * <p>外部依赖约定：无可用模型端点时，本测试经 {@link ModelEndpoint#assumeUp()} 优雅跳过（CI 不失败）；
 * 端点可用时则真实发一次对话请求并断言返回非空——这是后续所有真实模型场景的验证基线。</p>
 */
@SpringBootTest
class LlamaSmokeTest {

    @Autowired
    private ChatClient chatClient;

    @Test
    void chatWithLocalLlamaServer() {
        ModelEndpoint.assumeUp();

        String question = "用一句话介绍你自己，并说明你当前是什么模型。";

        System.out.println("=== 冒烟：Spring AI 对话 llama-server ===");
        System.out.println("【输入】" + question);

        String answer = chatClient.prompt(question).call().content();

        System.out.println("【输出】" + answer);
        assertThat(answer).as("模型应返回非空回答").isNotBlank();
    }
}
