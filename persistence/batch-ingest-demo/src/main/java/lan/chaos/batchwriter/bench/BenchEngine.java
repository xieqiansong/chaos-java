package lan.chaos.batchwriter.bench;

import lan.chaos.batchwriter.writer.BatchWriter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * 压测引擎：N 个汇聚线程以目标速率向一个 Writer 注入数据，跑满时长后汇总指标。
 * 供 {@link BenchRunner}（java -jar）与 SpringBootTest 复用。
 */
public final class BenchEngine {

    private BenchEngine() {
    }

    public static BenchResult run(BatchWriter<String> writer, BenchOptions o) {
        writer.start();
        AtomicBoolean running = new AtomicBoolean(true);
        CountDownLatch done = new CountDownLatch(o.threads);
        double perThreadInterval = o.rate / (double) o.threads;

        long start = System.nanoTime();
        for (int t = 0; t < o.threads; t++) {
            final int ti = t;
            new Thread(() -> {
                long next = System.nanoTime();
                long i = 0;
                try {
                    while (running.get()) {
                        if (!o.flood) {
                            long now = System.nanoTime();
                            if (now < next) {
                                LockSupport.parkNanos(next - now);
                                continue;
                            }
                            next += 1_000_000_000.0 / perThreadInterval;
                        }
                        writer.write("item-" + ti + "-" + (i++));
                    }
                } finally {
                    done.countDown();
                }
            }, "bench-producer-" + t).start();
        }

        try {
            Thread.sleep(o.durationSec * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        running.set(false);
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long elapsedNs = System.nanoTime() - start;
        double elapsedSec = elapsedNs / 1_000_000_000.0;

        long written = writer.itemsWritten();
        long calls = writer.redisCalls();
        long dropped = writer.dropped();
        long errors = writer.errors();
        double itemsPerSec = written / elapsedSec;
        double redisPerSec = calls / elapsedSec;
        return new BenchResult(writer.name(), written, itemsPerSec, calls, redisPerSec,
                writer.avgBatchSize(), dropped, errors);
    }
}