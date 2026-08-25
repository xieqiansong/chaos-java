package lan.chaos.ratelimiter.bench;

import lan.chaos.ratelimiter.RateLimiter;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * 压测引擎：对一组限流器实例（N 个"节点"）跑压力测试，返回结构化结果。
 * 供 {@link BenchRunner}（java -jar 命令行模式）与 SpringBootTest 复用，避免重复实现。
 */
public final class BenchEngine {

    private BenchEngine() {
    }

    public static BenchResult run(RateLimiter[] limiters, BenchOptions o) {
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicLong allowed = new AtomicLong();
        AtomicLong denied = new AtomicLong();
        AtomicLong sampleCount = new AtomicLong();
        ConcurrentLinkedQueue<Long> samples = new ConcurrentLinkedQueue<>();
        CountDownLatch done = new CountDownLatch(o.threads);
        AtomicLong reqIdx = new AtomicLong();
        int maxSamples = 2_000_000;

        long start = System.nanoTime();
        for (int t = 0; t < o.threads; t++) {
            new Thread(() -> {
                long next = System.nanoTime();
                double intervalNs = o.flood ? 0 : 1_000_000_000.0 / (o.qps / (double) o.threads);
                try {
                    while (running.get()) {
                        if (!o.flood) {
                            long now = System.nanoTime();
                            if (now < next) {
                                LockSupport.parkNanos(next - now);
                                continue;
                            }
                            next += intervalNs;
                        }
                        long idx = reqIdx.getAndIncrement();
                        int node = routeNode(idx, o);
                        String tenant = "tenant" + (idx % o.tenants);
                        long t0 = System.nanoTime();
                        boolean ok = limiters[node].tryAcquire(tenant);
                        long lat = System.nanoTime() - t0;
                        if (ok) {
                            allowed.incrementAndGet();
                        } else {
                            denied.incrementAndGet();
                        }
                        if (sampleCount.get() < maxSamples) {
                            samples.add(lat);
                            sampleCount.incrementAndGet();
                        }
                    }
                } finally {
                    done.countDown();
                }
            }).start();
        }

        try {
            Thread.sleep(o.durationSec * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        running.set(false);
        try {
            done.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        long elapsedNs = System.nanoTime() - start;
        double elapsedSec = elapsedNs / 1_000_000_000.0;

        long totalReq = allowed.get() + denied.get();
        long totalRedis = 0;
        long totalLocal = 0;
        for (RateLimiter l : limiters) {
            totalRedis += l.redisCalls();
            totalLocal += l.localAllows();
        }
        double qps = allowed.get() / elapsedSec;
        double theoretical = o.limit * o.durationSec;
        double overLimitPct = theoretical <= 0 ? 0 : (allowed.get() - theoretical) / theoretical * 100.0;
        long avgNs = samples.isEmpty() ? 0 : samples.stream().mapToLong(Long::longValue).sum() / samples.size();
        long p99Ns = percentile(samples, 0.99);
        double localHitPct = allowed.get() == 0 ? 0 : totalLocal * 100.0 / allowed.get();

        return new BenchResult(totalReq, allowed.get(), denied.get(), qps,
                avgNs / 1000.0, p99Ns / 1000.0, totalRedis,
                totalRedis / elapsedSec, totalLocal, localHitPct, overLimitPct);
    }

    private static int routeNode(long idx, BenchOptions o) {
        if (o.skew >= 0) {
            return ThreadLocalRandom.current().nextDouble() < o.skew
                    ? 0
                    : 1 + ThreadLocalRandom.current().nextInt(o.nodes - 1);
        }
        return (int) (idx % o.nodes);
    }

    private static long percentile(ConcurrentLinkedQueue<Long> q, double pc) {
        if (q.isEmpty()) {
            return 0;
        }
        long[] a = q.stream().mapToLong(Long::longValue).toArray();
        Arrays.sort(a);
        return a[Math.min(a.length - 1, (int) (pc * a.length))];
    }
}