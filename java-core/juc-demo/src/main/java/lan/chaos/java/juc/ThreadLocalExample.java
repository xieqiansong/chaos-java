package lan.chaos.java.juc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadLocalExample {

    // 创建 ThreadLocal 实例，存储每个线程的私有计数器
    private static final ThreadLocal<Integer> threadCounter = ThreadLocal.withInitial(() -> 0);

    // 创建另一个 ThreadLocal 存储线程名
    private static final ThreadLocal<String> threadName = new ThreadLocal<>();

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // 提交 5 个任务到线程池
        for (int i = 1; i <= 5; i++) {
            executor.execute(new WorkerThread(i));
        }
        executor.shutdown();
    }

    static class WorkerThread implements Runnable {
        private final int taskId;

        public WorkerThread(int taskId) {
            this.taskId = taskId;
        }

        @Override
        public void run() {
            try {
                // 设置当前线程名称（不同任务共享线程时会被覆盖）
                threadName.set(Thread.currentThread().getName());

                // 模拟线程处理过程
                for (int i = 0; i < 3; i++) {
                    // 访问线程私有计数器
                    int counter = incrementCounter();
                    System.out.printf("Task-%d | Counter: %d | Thread: %s\n",
                            taskId, counter, threadName.get());

                    Thread.sleep(100); // 模拟处理耗时
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // ⚠️ 重要：清理 ThreadLocal 防止内存泄漏（尤其线程池场景）
                threadCounter.remove();
                threadName.remove();
            }
        }

        // 线程安全的自增操作（每个线程独立计数）
        private int incrementCounter() {
            threadCounter.set(threadCounter.get() + 1);
            return threadCounter.get();
        }
    }
}
