package lan.chaos.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 最小 MCP 服务端。
 *
 * <p>WHY：MCP（Model Context Protocol）把"能力"从模型应用中解耦出来——
 * 服务端只管按协议暴露工具，任何支持 MCP 的客户端（Spring AI、Claude Desktop、IDE 插件…）
 * 都能直接接入调用，不必为每个应用重写一遍集成代码。</p>
 *
 * <p>本服务端以 SSE 传输对外提供工具，供 {@code jdk17-springai-demo} 的 MCP 客户端连接。</p>
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
