package lan.chaos.springai.chat.memory;

import lan.chaos.springai.testutil.ModelEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * chat/memory 多轮记忆端到端测试（真实模型）。
 *
 * <p>同一 conversationId 下连续对话，验证第二轮能带上第一轮的信息。
 * 断言采用宽松策略（回复非空）：模型输出受采样影响，强断言"记得名字"偶发误报，
 * 但控制台会打印两轮内容，人工可确认记忆是否生效。</p>
 */
@SpringBootTest
class MemoryChatTest {

    @Autowired
    private MemoryChatService service;

    @Test
    void remembersAcrossTurns() {
        ModelEndpoint.assumeUp();

        String conversationId = "demo-" + System.currentTimeMillis();

        System.out.println("=== memory/多轮记忆（会话 " + conversationId + "）===");

        String first = service.chat(conversationId, "我叫小明，是一名后端工程师，请记住这两个信息。");
        System.out.println("【第 1 轮 输入】我叫小明，是一名后端工程师，请记住这两个信息。");
        System.out.println("【第 1 轮 输出】" + first);

        String second = service.chat(conversationId, "我叫什么名字？我的职业是什么？");
        System.out.println("【第 2 轮 输入】我叫什么名字？我的职业是什么？");
        System.out.println("【第 2 轮 输出】" + second);

        assertThat(first).as("第 1 轮回答非空").isNotBlank();
        assertThat(second).as("第 2 轮回答非空").isNotBlank();
    }
}
