package lan.chaos.springai.testutil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 测试工具：本地 llama-server 探活。
 *
 * <p>外部依赖约定：所有真实模型测试都通过 {@link #assumeUp()} 探活——
 * llama-server 未启动时 assumption 失败（测试跳过，不红不失败），CI 无外部依赖也能通过。</p>
 */
public final class LlamaServer {

    public static final String BASE_URL = "http://localhost:30040";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private LlamaServer() {
    }

    /** llama-server /health 探活：已启动返回 200，加载中 503，未启动连接失败。 */
    public static boolean isUp() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/health"))
                    .GET()
                    .timeout(REQUEST_TIMEOUT)
                    .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /** 未启动时跳过当前测试；已启动则继续。 */
    public static void assumeUp() {
        assumeTrue(isUp(), "本地 llama-server(" + BASE_URL + ") 未启动，跳过真实模型调用");
    }
}
