package lan.chaos.virtualthread.structured;

import lan.chaos.virtualthread.common.util.IoSimulator;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;

/**
 * WHY：虚拟线程数量可以很大，但「谁等谁」的关系容易失控；
 * StructuredTaskScope 把子任务的生命周期收束到父作用域：全部完成/失败传播/超时统一判定。
 * 演示三种行为：成功并行（总耗时≈单个任务）、失败传播（任一失败整体失败）、超时（joinUntil 到点抛超时）。
 */
public class StructuredConcurrency {

    /** 成功并行：两个任务各阻塞 ioMillis，总耗时接近单个任务时长。 */
    public long runParallelSuccess(long ioMillis) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var a = scope.fork(() -> {
                IoSimulator.ioBlock(ioMillis);
                return ioMillis;
            });
            var b = scope.fork(() -> {
                IoSimulator.ioBlock(ioMillis);
                return ioMillis;
            });
            scope.join();
            scope.throwIfFailed(e -> new ExecutionException(e));
            return a.get() + b.get();
        }
    }

    /** 失败传播：任务A阻塞后返回，任务B立即抛异常 → ShutdownOnFailure 取消任务A，throwIfFailed 以 ExecutionException 聚合抛出。 */
    public boolean runFailurePropagation() throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            scope.fork(() -> {
                IoSimulator.ioBlock(20);
                return 1;
            });
            scope.fork(() -> {
                throw new IllegalStateException("任务B模拟异常");
            });
            scope.join();
            scope.throwIfFailed(e -> new ExecutionException(e));
            return true;
        }
    }

    /** 超时：任务阻塞 200ms，joinUntil(50ms 后) 到点抛 TimeoutException。 */
    public boolean runTimeout() throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            scope.fork(() -> {
                IoSimulator.ioBlock(200);
                return 1;
            });
            scope.joinUntil(Instant.now().plus(Duration.ofMillis(50)));
            scope.throwIfFailed(e -> new ExecutionException(e));
            return true;
        }
    }

    public void demo() {
        try {
            long io = 50;
            long start = System.nanoTime();
            long sum = runParallelSuccess(io);
            long cost = (System.nanoTime() - start) / 1_000_000;
            System.out.println("[输出] 成功并行：两个任务各模拟IO " + io + "ms，汇总=" + sum + "，总耗时=" + cost + "ms（≈单个任务时长）");
        } catch (Exception e) {
            System.out.println("[输出] 成功并行异常：" + e);
        }
        try {
            runFailurePropagation();
            System.out.println("[输出] 失败传播：未抛出异常（不符合预期）");
        } catch (ExecutionException e) {
            System.out.println("[输出] 失败传播：throwIfFailed 抛 ExecutionException，整体失败（符合预期）");
        } catch (Exception e) {
            System.out.println("[输出] 失败传播：其他异常 " + e);
        }
        try {
            runTimeout();
            System.out.println("[输出] 超时：未抛出异常（不符合预期）");
        } catch (TimeoutException e) {
            System.out.println("[输出] 超时：joinUntil 到点抛 TimeoutException（符合预期）");
        } catch (Exception e) {
            System.out.println("[输出] 超时：其他异常 " + e);
        }
    }
}
