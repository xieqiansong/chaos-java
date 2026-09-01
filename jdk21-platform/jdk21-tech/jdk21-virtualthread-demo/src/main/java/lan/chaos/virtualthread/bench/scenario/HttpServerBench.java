package lan.chaos.virtualthread.bench.scenario;

import com.sun.net.httpserver.HttpServer;
import lan.chaos.virtualthread.bench.BenchEngine;
import lan.chaos.virtualthread.bench.BenchOptions;
import lan.chaos.virtualthread.bench.BenchScenario;
import lan.chaos.virtualthread.bench.ExecutorFactory;
import lan.chaos.virtualthread.common.constant.ExecutorMode;
import lan.chaos.virtualthread.common.constant.Scenario;
import lan.chaos.virtualthread.common.model.BenchCase;
import lan.chaos.virtualthread.common.model.BenchResult;
import lan.chaos.virtualthread.common.util.IoSimulator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 压测 E：HTTP 服务端 平台线程 vs 虚拟线程（等价于 Tomcat 的 executor 替换）。
 *
 * <p>WHY 用 JDK 内置 {@link HttpServer} 而不是 Spring Boot + Tomcat：
 * 同一个服务端代码、同一个客户端负载，唯一变量就是 {@code setExecutor()} 传进去的执行器——
 * 零外部依赖、无框架干扰，结论最干净。这与 Spring Boot 3.2+ 的
 * {@code spring.threads.virtual.enabled=true}（给 Tomcat 换上虚拟线程 executor）是同一件事。
 *
 * <p>客户端同样固定用虚拟线程执行器发请求，确保压测端不会先于服务端成为瓶颈；
 * 否则测到的是「客户端发不出那么多请求」，而非服务端的处理能力差异。
 */
public class HttpServerBench implements BenchScenario {

    private static final String PATH = "/vt/bench";
    private static final byte[] BODY = "OK".getBytes(StandardCharsets.UTF_8);
    private static final int REQUEST_COUNT = 3000;
    private static final int CONCURRENCY = 300;
    private static final long IO_MILLIS = 20;
    private static final int ROUNDS = 5;

    @Override
    public Scenario id() {
        return Scenario.BENCH_HTTP;
    }

    @Override
    public String conclusion() {
        return "HTTP 服务只换 executor：并发压过线程数后，平台池把请求堆在队列里排队（p99 先行劣化），"
                + "虚拟线程随到随处理；线程数配得越小、下游越慢，差距越大。";
    }

    @Override
    public List<BenchCase> run() {
        List<BenchCase> cases = new ArrayList<>();
        cases.add(runCase("服务端池 50 线程 / 并发 300", 50));
        cases.add(runCase("服务端池 200 线程 / 并发 300", 200));
        return cases;
    }

    private BenchCase runCase(String name, int serverThreads) {
        BenchOptions options = BenchOptions.builder()
                .taskCount(REQUEST_COUNT)
                .concurrency(CONCURRENCY)
                .platformThreads(serverThreads)
                .ioMillis(IO_MILLIS)
                .warmupRounds(1)
                .build();

        BenchResult platform = medianOfRounds(ExecutorMode.PLATFORM, options);
        BenchResult virtual = medianOfRounds(ExecutorMode.VIRTUAL, options);

        return new BenchCase(name, options.describe() + "（" + ROUNDS + " 轮取中位数）",
                "平台线程池(" + serverThreads + ")", "虚拟线程", platform, virtual);
    }

    /**
     * 本地回环 HTTP 压测波动明显（端口复用、OS 网络栈、GC 都会抖），实测单次采样能差出 ±25%，
     * 直接拿一轮数字出报告会导致结论反复。这里跑多轮取吞吐中位数。
     * 其余场景（纯内存模拟）波动在 3% 以内，不需要这么做。
     */
    private BenchResult medianOfRounds(ExecutorMode mode, BenchOptions options) {
        List<BenchResult> rounds = new ArrayList<>();
        for (int i = 0; i < ROUNDS; i++) {
            rounds.add(runServer(mode, options));
        }
        rounds.sort(Comparator.comparingDouble(BenchResult::throughputPerSec));
        return rounds.get(rounds.size() / 2);
    }

    private BenchResult runServer(ExecutorMode mode, BenchOptions options) {
        ExecutorService serverExecutor = ExecutorFactory.create(mode, options);

        HttpServer server;
        try {
            // backlog 给足：并发建连阶段若 backlog 偏小，会偶发连接被拒，把「建连失败」误记成业务失败
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 1024);
        } catch (IOException e) {
            serverExecutor.shutdownNow();
            throw new IllegalStateException("启动压测用 HTTP 服务失败", e);
        }
        server.createContext(PATH, exchange -> {
            try {
                IoSimulator.ioBlock(options.getIoMillis());
                exchange.sendResponseHeaders(200, BODY.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(BODY);
                }
            } catch (IOException e) {
                // 多是客户端已断开：此时响应可能已发出，再 sendResponseHeaders 会抛 IllegalStateException。
                // 这类失败由客户端侧计入 failed，服务端不二次响应。
            } finally {
                exchange.close();
            }
        });
        server.setExecutor(serverExecutor);
        server.start();

        String url = "http://127.0.0.1:" + server.getAddress().getPort() + PATH;
        // 两个执行器都要持有引用并在 finally 关闭：HttpClient 的回调执行器若只在 builder 里 new 出来，
        // 就再也关不掉了，连跑多轮会不断累积
        ExecutorService clientCallbackExecutor = Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService clientLoadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                // 客户端也用虚拟线程：保证压测端不会先成为瓶颈
                .executor(clientCallbackExecutor)
                .build();
        try {
            return BenchEngine.run(mode.desc(), clientLoadExecutor, options, index -> {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("非预期响应码: " + response.statusCode());
                }
            });
        } finally {
            clientLoadExecutor.shutdownNow();
            clientCallbackExecutor.shutdownNow();
            server.stop(0);
            serverExecutor.shutdownNow();
        }
    }
}
