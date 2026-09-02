package lan.chaos.springai.testutil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 测试工具：当前"模型端点"是否可用（自动适配本地 llama-server / 云端 DeepSeek）。
 *
 * <p>WHY：真实模型测试依赖外部环境，统一由 {@link #assumeUp()} 探活——
 * 环境不具备时 assumption 失败（测试跳过，不红不失败），保证 CI 无外部依赖也能通过。</p>
 *
 * <p>两种模式由 {@code spring.profiles.active} 决定：
 * <ul>
 *   <li>本地模式（默认）：探活 {@code localhost:30040} 的 llama-server；</li>
 *   <li>deepseek 模式：要求环境变量 DEEPSEEK_API_KEY 已配置且 API 可达。</li>
 * </ul></p>
 */
public final class ModelEndpoint {

    private static final String DEEPSEEK_PROFILE = "deepseek";
    private static final String DEEPSEEK_MODELS_URL = "https://api.deepseek.com/models";
    private static final String API_KEY_ENV = "DEEPSEEK_API_KEY";
    /** 本地 bge-m3 embedding 服务（llama-server --embeddings）。 */
    private static final String EMBEDDING_BASE_URL = "http://localhost:30041";
    /** 本地最小 MCP 服务端（jdk17-mcp-server-demo）。 */
    private static final String MCP_SERVER_HOST = "localhost";
    private static final int MCP_SERVER_PORT = 30052;
    private static final String MCP_PROFILE = "mcp";

    private ModelEndpoint() {
    }

    /** 当前端点是否可用（本地探活或云端 API 可达）。 */
    public static boolean isUp() {
        return isDeepSeekProfile() ? isDeepSeekUp() : LlamaServer.isUp();
    }

    /** 端点不可用时跳过当前测试；可用则继续。 */
    public static void assumeUp() {
        assumeTrue(isUp(), "无可用的模型端点（本地 llama-server 未启动，或云端未配置 DEEPSEEK_API_KEY），跳过真实模型调用");
    }

    /** Embedding 服务是否可用（本地 bge-m3）。 */
    public static boolean isEmbeddingUp() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(EMBEDDING_BASE_URL + "/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /** Embedding 服务不可用时跳过（RAG 依赖它做向量化与检索）。 */
    public static void assumeEmbeddingUp() {
        assumeTrue(isEmbeddingUp(), "本地 embedding 服务(" + EMBEDDING_BASE_URL + ") 未启动，跳过 RAG 测试");
    }

    /**
     * MCP 服务端是否可用。
     *
     * <p>用 TCP 连接探测而非 HTTP 请求：MCP 的 /sse 是长连接，HTTP 请求会一直挂起直到超时，
     * 无法用它判断服务是否就绪。</p>
     */
    public static boolean isMcpServerUp() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(MCP_SERVER_HOST, MCP_SERVER_PORT), 2000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * MCP 工具可用时继续，否则跳过。
     *
     * <p>两个条件缺一不可：服务端要可达，且必须启用 {@code mcp} profile——
     * 客户端默认关闭，未启用时 ChatClient 不会挂载远程工具，即使服务端在跑也调不通。</p>
     */
    public static void assumeMcpServerUp() {
        assumeTrue(isProfileActive(MCP_PROFILE) && isMcpServerUp(),
                "需启用 " + MCP_PROFILE + " profile 且本地 MCP 服务端("
                        + MCP_SERVER_HOST + ":" + MCP_SERVER_PORT + ") 已启动，跳过 MCP 测试");
    }

    private static boolean isDeepSeekProfile() {
        return isProfileActive(DEEPSEEK_PROFILE);
    }

    /** 判断 profile 是否激活（兼容 -Dspring.profiles.active 与环境变量 SPRING_PROFILES_ACTIVE）。 */
    private static boolean isProfileActive(String profile) {
        String profiles = System.getProperty("spring.profiles.active", "");
        if (profiles.isEmpty()) {
            profiles = System.getenv("SPRING_PROFILES_ACTIVE") == null ? "" : System.getenv("SPRING_PROFILES_ACTIVE");
        }
        return profiles.contains(profile);
    }

    /** 云端可用性：API Key 已配置且 /models 返回 200（规避无网/欠费等环境直接失败）。 */
    private static boolean isDeepSeekUp() {
        String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(DEEPSEEK_MODELS_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .timeout(Duration.ofSeconds(8))
                    .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
