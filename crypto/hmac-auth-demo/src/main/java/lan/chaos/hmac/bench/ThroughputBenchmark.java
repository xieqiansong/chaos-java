package lan.chaos.hmac.bench;

import lan.chaos.hmac.core.HmacSigner;
import lan.chaos.hmac.core.SecretKeyStore;
import lan.chaos.hmac.model.ReportRequest;
import lan.chaos.hmac.verify.ReplayGuard;
import lan.chaos.hmac.verify.RequestVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 场景 D：上报鉴权两模式吞吐对比。
 *
 * <pre>
 *   旧模式：请求带 Token → 每请求 1 次 Redis 读校验（模拟约 1ms 往返）
 *   新模式：HMAC 签名 → 本地验签（0 往返）
 * </pre>
 *
 * 量化主线：消除每请求网络往返 → QPS 数量级提升（呼应 Q1：1000+ → 10000+）。
 */
public final class ThroughputBenchmark {

    private static final String SECRET = "bench-secret";

    private final int totalRequests;
    private final int threads;
    private final long redisLatencyMicros;

    public ThroughputBenchmark(int totalRequests, int threads, long redisLatencyMicros) {
        this.totalRequests = totalRequests;
        this.threads = threads;
        this.redisLatencyMicros = redisLatencyMicros;
    }

    public void run() {
        List<ReportRequest> requests = buildRequests(totalRequests);
        Result legacy = bench(requests, true);
        Result hmac = bench(requests, false);
        print(legacy, hmac);
    }

    private List<ReportRequest> buildRequests(int n) {
        List<ReportRequest> list = new ArrayList<>(n);
        long now = System.currentTimeMillis() / 1000L;
        for (int i = 0; i < n; i++) {
            ReportRequest req = new ReportRequest();
            req.setDeviceId("dev-" + i);
            req.setPath("/v1/report");
            req.setTimestamp(now);
            req.setNonce(HmacSigner.newNonce());
            req.setBatchNo("batch-" + i);
            req.setBody("{\"i\":" + i + "}");
            req.setSign(HmacSigner.sign(SECRET, "POST", req.getPath(),
                    req.getTimestamp(), req.getNonce(), req.getBody()));
            list.add(req);
        }
        return list;
    }

    private Result bench(List<ReportRequest> requests, boolean withRedisRead) {
        final SecretKeyStore keyStore = new SecretKeyStore(SECRET);
        final ReplayGuard guard = new ReplayGuard(Integer.MAX_VALUE / 2, 60_000L);
        final RequestVerifier verifier = new RequestVerifier(keyStore, guard, 300);
        final RedisReadSimulator redis = new RedisReadSimulator(redisLatencyMicros);
        final long[] costs = new long[requests.size()];
        final AtomicInteger idx = new AtomicInteger();
        final CountDownLatch done = new CountDownLatch(requests.size());

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        long start = System.nanoTime();
        for (ReportRequest req : requests) {
            pool.submit(() -> {
                int i = idx.getAndIncrement();
                long t0 = System.nanoTime();
                verifier.verify(req, req.getTimestamp() + 1);
                if (withRedisRead) {
                    redis.read();
                }
                costs[i] = System.nanoTime() - t0;
                done.countDown();
            });
        }
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long totalNanos = System.nanoTime() - start;
        pool.shutdown();
        return Result.of(costs, totalNanos);
    }

    private void print(Result legacy, Result hmac) {
        System.out.println();
        System.out.println("== 场景 D：上报鉴权吞吐对比（总请求=" + totalRequests
                + "，线程=" + threads + "，模拟Redis延迟=" + redisLatencyMicros + "us） ==");
        System.out.printf("%-34s %12s %12s %12s %12s%n",
                "mode", "elapsed(ms)", "QPS", "avg(us)", "p99(us)");
        System.out.printf("%-34s %12.0f %12.0f %12.1f %12.1f%n",
                "old: token + redis-read(req)", legacy.totalMillis(), legacy.qps,
                legacy.avgMicros(), legacy.p99Micros());
        System.out.printf("%-34s %12.0f %12.0f %12.1f %12.1f%n",
                "new: hmac local-verify(0-rtt)", hmac.totalMillis(), hmac.qps,
                hmac.avgMicros(), hmac.p99Micros());
        System.out.printf("%nQPS boost: %.1fx ; avg latency down: %.1f%% (remove per-request network round-trip)%n",
                hmac.qps / legacy.qps, (1 - hmac.avgMicros() / legacy.avgMicros()) * 100);
    }

    /** 基准结果：avg / p99 / QPS。 */
    static final class Result {
        final double avgNanos;
        final double p99Nanos;
        final double qps;
        final long totalNanos;

        private Result(double avgNanos, double p99Nanos, double qps, long totalNanos) {
            this.avgNanos = avgNanos;
            this.p99Nanos = p99Nanos;
            this.qps = qps;
            this.totalNanos = totalNanos;
        }

        static Result of(long[] costs, long totalNanos) {
            long[] sorted = costs.clone();
            Arrays.sort(sorted);
            int n = sorted.length;
            double sum = 0;
            for (long c : sorted) {
                sum += c;
            }
            double avg = sum / n;
            double p99 = sorted[(int) (n * 0.99)];
            double qps = n * 1e9 / totalNanos;
            return new Result(avg, p99, qps, totalNanos);
        }

        double totalMillis() {
            return totalNanos / 1e6;
        }

        double avgMicros() {
            return avgNanos / 1e3;
        }

        double p99Micros() {
            return p99Nanos / 1e3;
        }
    }
}
