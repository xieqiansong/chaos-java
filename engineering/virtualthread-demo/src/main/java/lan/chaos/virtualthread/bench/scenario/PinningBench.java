package lan.chaos.virtualthread.bench.scenario;

import lan.chaos.virtualthread.bench.BenchEngine;
import lan.chaos.virtualthread.bench.BenchOptions;
import lan.chaos.virtualthread.bench.BenchScenario;
import lan.chaos.virtualthread.common.constant.ExecutorMode;
import lan.chaos.virtualthread.common.constant.Scenario;
import lan.chaos.virtualthread.common.model.BenchCase;
import lan.chaos.virtualthread.common.model.BenchResult;
import lan.chaos.virtualthread.common.util.IoSimulator;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 压测 C：synchronized pinning 的吞吐代价（换虚拟线程但不换锁 = 白换）。
 *
 * <p>WHY 这一组：JDK 21 里虚拟线程在 synchronized 临界区内阻塞时会被「钉住」——
 * 不卸载、占住载体线程不放，并发能力退化到 ≈ 载体线程数（默认等于 CPU 核数）。
 * 生产上最容易踩的坑就是：把线程池换成虚拟线程了，但路径上还挂着 synchronized 的
 * 同步工具类 / 三方库，收益直接归零。
 *
 * <p>设计要点：每个任务持有**自己的**锁，排除锁竞争，让差异只来自 pinning 本身。
 * 两组都用虚拟线程执行器（对照组不是平台线程），因此差距纯粹由锁类型造成。
 */
public class PinningBench implements BenchScenario {

    private static final int TASK_COUNT = 800;
    private static final long IO_MILLIS = 20;

    @Override
    public Scenario id() {
        return Scenario.BENCH_PINNING;
    }

    @Override
    public String conclusion() {
        return "pinning 会把虚拟线程打回平台线程：峰值并发被锁死在载体线程数，吞吐差出一个数量级。"
                + "换虚拟线程必须同步排查 synchronized 临界区内的阻塞调用（JDK 24 的 JEP 491 才修）。";
    }

    @Override
    public List<BenchCase> run() {
        BenchOptions options = BenchOptions.builder()
                .taskCount(TASK_COUNT)
                .concurrency(TASK_COUNT)
                .ioMillis(IO_MILLIS)
                .warmupRounds(1)
                .build();

        // 基线：临界区内阻塞被 pin 住载体线程
        Object[] monitors = new Object[TASK_COUNT];
        for (int i = 0; i < TASK_COUNT; i++) {
            monitors[i] = new Object();
        }
        BenchResult pinned = BenchEngine.run("虚拟线程 + synchronized(阻塞)",
                ExecutorMode.VIRTUAL, options, index -> IoSimulator.syncBlock(monitors[index], IO_MILLIS));

        // 对照：ReentrantLock 临界区内阻塞，正常卸载
        ReentrantLock[] locks = new ReentrantLock[TASK_COUNT];
        for (int i = 0; i < TASK_COUNT; i++) {
            locks[i] = new ReentrantLock();
        }
        BenchResult unlocked = BenchEngine.run("虚拟线程 + ReentrantLock(阻塞)",
                ExecutorMode.VIRTUAL, options, index -> {
                    ReentrantLock lock = locks[index];
                    lock.lock();
                    try {
                        IoSimulator.ioBlock(IO_MILLIS);
                    } finally {
                        lock.unlock();
                    }
                });

        // 不复用 options.describe()：本场景两组都是虚拟线程，带上「平台池线程数」会误导
        String params = "任务数=" + TASK_COUNT + " 并发=" + TASK_COUNT + " 锁内阻塞=" + IO_MILLIS
                + "ms（两组均为虚拟线程，不涉及平台池）";
        return List.of(new BenchCase("锁内阻塞 20ms，每任务独立锁", params,
                "synchronized(pinning)", "ReentrantLock(可卸载)", pinned, unlocked));
    }
}
