package lan.chaos.springai.mcp;

import lan.chaos.springai.testutil.ModelEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * mcp：MCP 远程工具调用测试（真实模型 + 本地 MCP 服务端）。
 *
 * <p>验证思路与本地工具一致：模型本身不知道服务端当前时间，
 * 回复里出现真实时间即证明<b>远端</b> MCP 工具被成功调用。</p>
 *
 * <p>未启用 mcp profile、或 MCP 服务端未启动时优雅跳过。</p>
 */
@SpringBootTest
class McpTest {

    @Autowired
    private McpChatService service;

    @Test
    void modelCallsRemoteMcpTool() {
        ModelEndpoint.assumeUp();
        ModelEndpoint.assumeMcpServerUp();

        String answer = service.chat("MCP 服务端现在的时间是几点？请调用工具获取后告诉我。");

        System.out.println("=== mcp/远程工具调用（获取服务端时间）===");
        System.out.println("【模型回复】" + answer);

        assertThat(answer).as("回复非空").isNotBlank();
        assertThat(answer).as("回复应包含工具返回的真实时间（年份 " + LocalDate.now().getYear() + "）")
                .contains(String.valueOf(LocalDate.now().getYear()));
    }

    @Test
    void modelUsesMultiParamRemoteTool() {
        ModelEndpoint.assumeUp();
        ModelEndpoint.assumeMcpServerUp();

        String answer = service.chat("请用 MCP 服务端的加法工具算一下 123 加 456 等于多少？");

        System.out.println("=== mcp/多参数远程工具（加法）===");
        System.out.println("【模型回复】" + answer);

        assertThat(answer).as("回复非空").isNotBlank();
        assertThat(answer).as("模型应调用远程加法工具得出 579").contains("579");
    }
}
