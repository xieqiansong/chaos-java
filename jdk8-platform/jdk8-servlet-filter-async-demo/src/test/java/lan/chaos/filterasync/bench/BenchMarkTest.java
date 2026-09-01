package lan.chaos.filterasync.bench;

import com.sun.management.OperatingSystemMXBean;
import lan.chaos.filterasync.DemoApp;
import lan.chaos.filterasync.service.ReportSink;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自包含基准（一条 {@code mvn test} 跑完）：启动两次内嵌 Tomcat，
 * 分别用 {@code mode=controller-sync}（完整 MVC）与 {@code mode=filter-async}（Filter 提前返回），
 * 各自用并发 HTTP 客户端打 {@code /api/report}。
 *
 * <p>下游「异步提交即返回」刻意保持极轻（仅计数），聚焦「链路」CPU 成本，
 * 与入库/业务耗时无关。对比指标：吞吐(req/s)、p50/p99 延迟、Tomcat 忙线程峰值、进程 CPU。
 * 结果写入 {@code target/bench-results.md}。
 */
class BenchMarkTest {

    private static final int DURATION_SEC = 12;
    private static final int CONCURRENCY = 64;
    private static final String URL_PATH = "/api/report";
    private static final String BODY = "{\"id\":\"term-12345\",\"payload\":\"x\"}";

    @Test
    void benchAllModes() throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (String mode : new String[]{"controller-sync", "filter-async"}) {
            try (ConfigurableApplicationContext ctx = boot(mode)) {
                int port = ctx.getEnvironment().getProperty("local.server.port", Integer.class);
                ReportSink sink = ctx.getBean(ReportSink.class);
                BenchResult r = runLoad("http://localhost:" + port + URL_PATH);
                rows.add(new String[]{
                        mode,
                        String.format(Locale.ROOT, "%.1f", r.requestsPerSec),
                        String.valueOf(r.p50Ms),
                        String.valueOf(r.p99Ms),
                        String.valueOf(r.errors),
                        r.maxBusyThreads < 0 ? "N/A" : String.valueOf(r.maxBusyThreads),
                        r.cpuPct < 0 ? "N/A" : String.format(Locale.ROOT, "%.1f", r.cpuPct)
                });
                System.out.printf(Locale.ROOT,
                        "[%-16s] req/s=%.1f p50=%dms p99=%dms errors=%d maxBusy=%s cpu=%.1f%%%n",
                        mode, r.requestsPerSec, r.p50Ms, r.p99Ms, r.errors,
                        r.maxBusyThreads < 0 ? "N/A" : String.valueOf(r.maxBusyThreads),
                        r.cpuPct < 0 ? -1 : r.cpuPct);
                // 下游异步任务应当被接收（仅计数，不应失败）；断言接收到流量即可，避免收尾时少量在途任务导致抖动
                if (sink.accepted() <= 0) {
                    throw new AssertionError(mode + " 下游未接收到任何请求，accepted=" + sink.accepted());
                }
            }
        }
        writeMarkdown("target/bench-results.md", rows,
                new String[]{"mode", "req/s", "p50(ms)", "p99(ms)", "errors", "maxBusyThreads", "cpuPct(%)"});
    }

    private static ConfigurableApplicationContext boot(String mode) {
        return (ConfigurableApplicationContext) new SpringApplicationBuilder(DemoApp.class)
                .properties("app.mode=" + mode, "server.port=0",
                        "logging.level.lan.chaos.filterasync=WARN")
                .run();
    }

    private static BenchResult runLoad(String url) throws Exception {
        RestTemplate rt = new RestTemplate();
        ((SimpleClientHttpRequestFactory) rt.getRequestFactory()).setConnectTimeout(2000);
        ((SimpleClientHttpRequestFactory) rt.getRequestFactory()).setReadTimeout(2000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(BODY, headers);

        long deadline = System.currentTimeMillis() + DURATION_SEC * 1000L;
        AtomicLong success = new AtomicLong();
        AtomicLong errors = new AtomicLong();
        List<List<Long>> perWorker = new ArrayList<>(CONCURRENCY);
        for (int i = 0; i < CONCURRENCY; i++) {
            perWorker.add(new ArrayList<>(4096));
        }

        // Tomcat 忙线程采样
        AtomicLong maxBusy = new AtomicLong(-1);
        AtomicBoolean sampling = new AtomicBoolean(true);
        Thread sampler = new Thread(() -> sampleBusy(maxBusy, sampling), "tomcat-busy-sampler");
        sampler.setDaemon(true);
        sampler.start();

        // 进程 CPU 采样
        OperatingSystemMXBean osBean = cpuBean();
        List<Double> cpuSamples = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            final List<Long> lat = perWorker.get(i);
            futures.add(pool.submit(() -> {
                while (System.currentTimeMillis() < deadline) {
                    long t0 = System.nanoTime();
                    try {
                        rt.postForEntity(url, entity, String.class);
                        success.incrementAndGet();
                        if (lat.size() < 2_000_000) {
                            lat.add((System.nanoTime() - t0) / 1_000_000L); // ns -> ms
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            }));
        }

        // CPU 采样（与压测并行）
        while (System.currentTimeMillis() < deadline) {
            if (osBean != null) {
                double c = osBean.getProcessCpuLoad();
                if (c >= 0) {
                    cpuSamples.add(c);
                }
            }
            Thread.sleep(1000);
        }

        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdownNow();
        sampling.set(false);

        // 合并延迟并排序
        List<Long> all = new ArrayList<>();
        for (List<Long> l : perWorker) {
            all.addAll(l);
        }
        all.sort(Long::compareTo);

        long total = success.get() + errors.get();
        BenchResult r = new BenchResult();
        r.requestsPerSec = total / (double) DURATION_SEC;
        r.p50Ms = pct(all, 0.50);
        r.p99Ms = pct(all, 0.99);
        r.errors = errors.get();
        r.maxBusyThreads = maxBusy.get();
        r.cpuPct = cpuSamples.isEmpty()
                ? -1
                : cpuSamples.stream().mapToDouble(Double::doubleValue).average().orElse(-1) * 100.0;
        return r;
    }

    private static void sampleBusy(AtomicLong maxBusy, AtomicBoolean running) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        while (running.get()) {
            try {
                Set<ObjectName> names = mbs.queryNames(new ObjectName("Tomcat:type=ThreadPool,*"), null);
                for (ObjectName n : names) {
                    Object v = mbs.getAttribute(n, "currentThreadsBusy");
                    if (v instanceof Number) {
                        maxBusy.accumulateAndGet(((Number) v).longValue(), Math::max);
                    }
                }
            } catch (Exception ignored) {
                // MBean 未就绪或已注销：忽略
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static OperatingSystemMXBean cpuBean() {
        try {
            return (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        } catch (Throwable t) {
            return null;
        }
    }

    private static long pct(List<Long> sorted, double p) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int idx = (int) Math.min(sorted.size() - 1, Math.ceil(p * sorted.size()) - 1);
        return sorted.get(Math.max(0, idx));
    }

    private static final class BenchResult {
        double requestsPerSec;
        long p50Ms;
        long p99Ms;
        long errors;
        long maxBusyThreads;
        double cpuPct;
    }

    private static void writeMarkdown(String path, List<String[]> rows, String[] header) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# bench-results (Servlet Filter 异步化压测)\n\n");
        sb.append("> 自包含 SpringBootTest：内嵌 Tomcat，下游「异步提交即返回」保持极轻，聚焦链路 CPU 成本。\n");
        sb.append("> 每模式 ").append(DURATION_SEC).append("s / ").append(CONCURRENCY).append(" 并发客户端。\n\n");
        sb.append("| ");
        for (String h : header) {
            sb.append(h).append(" | ");
        }
        sb.append("\n|");
        for (String ignored : header) {
            sb.append("---|");
        }
        sb.append("\n");
        for (String[] r : rows) {
            sb.append("| ");
            sb.append(String.join(" | ", r));
            sb.append(" |\n");
        }
        sb.append("\n> 指标说明：req/s 越高越好；p50/p99 越低越好；maxBusyThreads 反映 Tomcat 线程占用峰值；");
        sb.append("cpuPct 为压测期进程 CPU 均值。二者下游处理完全一致，差异来自是否走整条 MVC 链路。\n");
        Files.write(Paths.get(path), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
