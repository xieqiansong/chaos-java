package lan.chaos.springai.mcp;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * mcp：MCP 客户端——让模型调用"远端 MCP 服务端"暴露的工具。
 *
 * <p>WHY：与 tools 能力组的本地工具（{@code @Tool}）相比，MCP 工具运行在<b>独立进程</b>里，
 * 可被多个 AI 应用共享、独立部署和演进。而对客户端来说，接入方式几乎一样：
 * 把 MCP 提供的 {@link ToolCallbackProvider} 交给 ChatClient，模型侧完全无感知。</p>
 *
 * <p>用 {@link ObjectProvider} 注入而非直接依赖：MCP 客户端默认关闭（避免未启动服务端时
 * 连接失败拖垮整个应用上下文），此时拿不到工具提供者，ChatClient 退化为不带 MCP 工具的普通对话。</p>
 */
@Service
public class McpChatService {

    private final ChatClient chatClient;

    public McpChatService(ChatClient.Builder builder, ObjectProvider<ToolCallbackProvider> mcpToolProviders) {
        this.chatClient = builder
                .defaultToolCallbacks(mcpToolProviders.orderedStream().toArray(ToolCallbackProvider[]::new))
                .build();
    }

    /** 对话：模型可按需调用远端 MCP 工具。 */
    public String chat(String userMessage) {
        return chatClient.prompt(userMessage).call().content();
    }
}
