package lan.chaos.java.juc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下面是一个详细展示 ScheduledThreadPoolExecutor 多种调度方式的完整代码示例，包含五种典型使用场景
 */
public class ScheduledThreadPoolDemo {
    // 时间格式化器
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    public static volatile boolean isRunning = true;

    public static void main(String[] args) throws InterruptedException {
        // 1. 创建线程池（核心线程数=2）
        ScheduledThreadPoolExecutor scheduler =
                new ScheduledThreadPoolExecutor(2, new CustomThreadFactory());

        // 设置策略：取消任务后立即从队列移除（避免内存泄露）
        scheduler.setRemoveOnCancelPolicy(true);

        // 2. 创建计数器
        final AtomicInteger taskCounter = new AtomicInteger(0);

        // ============== 示例1：单次延迟任务 ==============
        Runnable onceTask = () -> printWithTime("单次任务执行 ✅");
        ScheduledFuture<?> onceFuture = scheduler.schedule(
                onceTask, 1, TimeUnit.SECONDS);

        // ============== 示例2：固定延迟周期任务 ==============
        Runnable fixedDelayTask = () -> {
            try {
                printWithTime("固定延迟任务开始 🛠️");
                Thread.sleep(800); // 模拟任务耗时
                printWithTime("固定延迟任务结束 ⏱️ 计数: "
                        + taskCounter.incrementAndGet());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        ScheduledFuture<?> fixedDelayFuture = scheduler.scheduleWithFixedDelay(
                // 初始延迟2秒，周期3秒
                fixedDelayTask, 2, 3, TimeUnit.SECONDS);

        // ============== 示例3：固定频率周期任务 ==============
        Runnable fixedRateTask = () -> {
            printWithTime("固定频率任务开始 ⚡ 计数: "
                    + taskCounter.incrementAndGet());
            try {
                Thread.sleep(1200); // 模拟任务耗时
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            printWithTime("固定频率任务结束 🏁");
        };
        ScheduledFuture<?> fixedRateFuture = scheduler.scheduleAtFixedRate(
                // 初始延迟2秒，周期2秒
                fixedRateTask, 2, 2, TimeUnit.SECONDS);

        // ============== 示例4：可取消任务 ==============
        Runnable cancelableTask = () ->
                printWithTime("⚠️ 此任务应被取消，但依然执行了！");
        ScheduledFuture<?> cancelableFuture = scheduler.schedule(
                cancelableTask, 5, TimeUnit.SECONDS);
        // 在任务执行前取消
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                boolean canceled = cancelableFuture.cancel(true);
                printWithTime("取消任务结果: " + (canceled ? "成功 ✔" : "失败 ✘"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // ============== 示例5：带返回值的调度任务 ==============
        Callable<Integer> callableTask = () -> {
            printWithTime("带返回值任务执行 📡");
            return ThreadLocalRandom.current().nextInt(100);
        };
        ScheduledFuture<Integer> resultFuture = scheduler.schedule(
                callableTask, 4, TimeUnit.SECONDS);

        // 获取返回值
        new Thread(() -> {
            try {
                Integer result = resultFuture.get();
                printWithTime("获取到返回值: " + result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // 6. 监控线程池队列
        new Thread(() -> {
            while (isRunning) {
                try {
                    printWithTime("任务队列大小: " + scheduler.getQueue().size());
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();

        // 7. 主线程等待30秒后关闭线程池
        TimeUnit.SECONDS.sleep(30);

        printWithTime("正在关闭线程池...");
        scheduler.shutdown();  // 停止接收新任务

        // 等待现有任务完成
        if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
            scheduler.shutdownNow(); // 强制取消剩余任务
        }
        printWithTime("线程池已完全关闭");
        isRunning = false;

    }

    // 带时间戳的打印方法
    private static void printWithTime(String message) {
        System.out.printf("[%s] [%s] %s%n",
                LocalDateTime.now().format(formatter),
                Thread.currentThread().getName(),
                message);
    }

    // 自定义线程工厂（添加更清晰的线程命名）
    static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("Scheduler-Worker-" + counter.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }
}
