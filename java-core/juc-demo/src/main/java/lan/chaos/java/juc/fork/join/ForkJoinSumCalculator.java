package lan.chaos.java.juc.fork.join;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;

/**
 * 以下是一个使用 Java 的 JUC ForkJoinTask 框架（具体为 RecursiveTask）实现并行计算 1~n 整数和的完整示例代码，包含详细注释
 */
public class ForkJoinSumCalculator extends RecursiveTask<Long> {
    private final long start;
    private final long end;
    // 阈值：小于此值时直接计算（根据实际场景调整）
    private long threshold = 1_000_000;

    public ForkJoinSumCalculator(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    @Override
    protected Long compute() {
        long length = end - start + 1;
        // 1. 若数据量小于阈值，直接计算
        if (length <= threshold) {
            return computeSequentially();
        }

        // 2. 拆分任务
        long mid = start + length / 2;
        ForkJoinSumCalculator leftTask = new ForkJoinSumCalculator(start, mid);
        ForkJoinSumCalculator rightTask = new ForkJoinSumCalculator(mid + 1, end);

        // 3. 异步执行左子任务（压入工作队列）
        leftTask.fork();
        // 4. 同步执行右子任务（当前线程执行）
        long rightResult = rightTask.compute();
        // 5. 获取左子任务结果（阻塞等待）
        long leftResult = leftTask.join();

        // 6. 合并结果
        return leftResult + rightResult;
    }

    // 顺序计算方法（适用于小任务）
    private long computeSequentially() {
        long sum = 0;
        for (long i = start; i <= end; i++) {
            sum += i;
        }
        return sum;
    }

    public static void main(String[] args) {
        // 1. 创建ForkJoin线程池
        ForkJoinPool pool = new ForkJoinPool();
        // 2. 提交主任务
        long n = 1_000_000_000L;
        ForkJoinSumCalculator task = new ForkJoinSumCalculator(1, n);
        task.setThreshold(10_000_000);
        long startTime = System.nanoTime();
        long result = pool.invoke(task);

        // 3. 输出结果
        System.out.printf("1~%d 的和 = %,d%n", n, result);
        System.out.printf("耗时: %,d ms%n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));

        // 4. 关闭线程池（非必需，但建议）
        pool.shutdown();
    }

}
