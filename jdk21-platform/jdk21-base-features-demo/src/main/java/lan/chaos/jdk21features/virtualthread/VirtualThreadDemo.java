package lan.chaos.jdk21features.virtualthread;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * 虚拟线程 Virtual Threads（JEP 444，JDK21 定稿）：由 JVM 调度、挂载在载体线程上的轻量级线程。
 *
 * <p>WHY：平台线程（OS 线程）成本高（默认栈 MB 级、数量千级封顶），高并发 I/O 场景（如百万连接）下线程数成为瓶颈。
 * 虚拟线程开销极小，可"每任务一线程"而不必复用线程池，代码保持简单的同步阻塞风格却获得高吞吐。
 * 关键 API：
 * <ul>
 *   <li>{@code Thread.startVirtualThread(Runnable)} 直接起一个虚拟线程；</li>
 *   <li>{@code Executors.newVirtualThreadPerTaskExecutor()} 每任务一个虚拟线程的 ExecutorService；</li>
 *   <li>{@code thread.isVirtual()} 判断是否虚拟线程。</li>
 * </ul>
 * 生产坑点：虚拟线程不适合做 CPU 密集计算；避免在虚拟线程里调用 {@code synchronized} 长时间持锁或 native 阻塞（会 pin 载体线程）。
 */
public class VirtualThreadDemo {

    public static void run() {
        // 1) 直接启动一个虚拟线程
        Thread vt = Thread.startVirtualThread(() ->
                System.out.println("运行在虚拟线程: " + Thread.currentThread()));
        try {
            vt.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("vt.isVirtual() = " + vt.isVirtual());

        // 2) 每任务一个虚拟线程：一万个并发任务也不怕
        AtomicInteger counter = new AtomicInteger();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            int n = 10_000;
            ArrayList<Future<Integer>> futures = IntStream.range(0, n)
                    .mapToObj(i -> executor.submit(counter::incrementAndGet))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            // 等待全部完成
            for (Future<Integer> f : futures) {
                f.get();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("提交 10000 个虚拟线程任务，全部完成计数=" + counter.get());
    }
}
