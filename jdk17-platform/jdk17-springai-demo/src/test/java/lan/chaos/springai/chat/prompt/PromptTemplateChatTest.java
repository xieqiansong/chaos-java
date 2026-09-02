package lan.chaos.springai.chat.prompt;

import lan.chaos.springai.testutil.ModelEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * chat/prompt 提示词模板对话测试（真实模型）。
 *
 * <p>验证：模板 + 变量 → 模型按模板约束作答。</p>
 */
@SpringBootTest
class PromptTemplateChatTest {

    @Autowired
    private PromptTemplateService service;

    @Test
    void templateDrivenChat() {
        ModelEndpoint.assumeUp();

        String template = """
                你是一位{role}。请以这个身份回答下面的问题，保持{style}。
                问题：{question}
                """;

        System.out.println("=== prompt/提示词模板 ===");
        String answer = service.chat(template, Map.of(
                "role", "资深 Java 架构师",
                "style", "专业且简洁",
                "question", "微服务拆分的最基本原则是什么？"));

        System.out.println("【模板】" + template);
        System.out.println("【输出】" + answer);
        assertThat(answer).as("模板驱动对话返回非空").isNotBlank();
    }
}
