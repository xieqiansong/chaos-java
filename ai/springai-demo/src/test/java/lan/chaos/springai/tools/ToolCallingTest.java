package lan.chaos.springai.tools;

import lan.chaos.springai.testutil.ModelEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * chat/tools 工具调用测试（真实模型）。
 *
 * <p>验证思路：模型本身不知道"现在几点"，无法凭空编出当前时间——
 * 因此回复中出现真实时间，就证明工具确实被调用了（比断言"调用了某个工具"更贴近业务语义）。</p>
 */
@SpringBootTest
class ToolCallingTest {

    @Autowired
    private ToolCallingService service;

    @Test
    void modelCallsCurrentTimeTool() {
        ModelEndpoint.assumeUp();

        ChatResponse response = service.chatResponse("现在几点了？");
        String text = response.getResult().getOutput().getText();

        System.out.println("=== tools/工具调用：获取当前时间 ===");
        System.out.println("【模型回复】" + text);
        System.out.println("【最终响应是否携带 toolCalls】" + !response.getResult().getOutput().getToolCalls().isEmpty());

        assertThat(text).as("回复非空").isNotBlank();

        String year = String.valueOf(LocalDate.now().getYear());
        assertThat(text).as("回复应包含工具返回的真实时间（年份 " + year + "）").contains(year);
    }

    @Test
    void modelExtractsParameterForAlarmTool() {
        ModelEndpoint.assumeUp();

        String text = service.chat("请帮我设一个明天早上七点半的闹钟。");

        System.out.println("=== tools/工具调用：参数抽取 ===");
        System.out.println("【模型回复】" + text);

        assertThat(text).as("回复非空").isNotBlank();
        assertThat(text).as("模型应把『早上七点半』抽取为 07:30 参数")
                .containsPattern("0?7[:：]30|七点半|7点半");
    }
}
