package lan.chaos.jdk11features.httpclient;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * 标准 HttpClient（JDK11，java.net.http 退出孵化转正）：同步/异步 HTTP 客户端，替代 HttpURLConnection。
 *
 * <p>WHY：{@code HttpURLConnection} API 古老、默认阻塞且难用；新 {@code HttpClient} 支持 HTTP/2、
 * 同步 {@code send} 与异步 {@code sendAsync}、响应式 BodyHandler。
 * 关键 API / 规则：
 * <ul>
 *   <li>{@code HttpClient.newHttpClient()} 或 {@code newBuilder()} 配置超时/代理/版本；</li>
 *   <li>{@code HttpRequest.newBuilder(uri).GET().build()} 构造请求；</li>
 *   <li>{@code client.send(req, BodyHandlers.ofString())} 同步；{@code sendAsync(...)} 返回 CompletableFuture。</li>
 * </ul>
 * 下面用 JDK 内置的 {@code com.sun.net.httpserver.HttpServer} 起一个本地服务做"真实收发"演示（无需外网）。
 */
public class HttpClientDemo {

    /** 起本地服务并真实 GET，返回响应体；调用方负责 stop 服务器 */
    public static String localPing() throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ping", exchange -> {
            byte[] body = "pong".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
            exchange.close();
        });
        server.start();

        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/ping")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String body = resp.body();

        server.stop(0);
        return body;
    }

    public static void run() {
        // 1) 构建请求（演示 API，不发真实外网）
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://example.com/")).GET().build();
        System.out.println("method=" + req.method() + ", uri=" + req.uri());

        // 2) 本地服务真实收发
        try {
            String body = localPing();
            System.out.println("本地 HttpServer 回包: " + body);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
