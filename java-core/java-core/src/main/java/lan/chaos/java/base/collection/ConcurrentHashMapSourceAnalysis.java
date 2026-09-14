package lan.chaos.java.base.collection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 能力二：ConcurrentHashMap 线程安全 Map（JDK7 Segment 分段锁 → JDK8 CAS + synchronized 锁首节点）。
 *
 * <p>WHY：多线程环境下 HashMap 不是线程安全的（put 可能导致死循环/数据丢失）。
 * Hashtable 用 synchronized 锁整个表，并发度极低。ConcurrentHashMap 通过细粒度锁
 * 实现高并发读写，是 Java 并发编程中最重要的数据结构之一。
 *
 * <p>关键 API：put / get / putIfAbsent / computeIfAbsent / merge。
 * JDK8 淘汰了 JDK7 的 Segment（16 个分段锁），改为：
 * <ul>
 *   <li>put 时 CAS 尝试写入空槽，冲突时 synchronized 锁链表/红黑树头节点</li>
 *   <li>get 全程无锁（volatile 保证可见性）</li>
 *   <li>扩容支持多线程协助迁移（transfer），避免单线程成为瓶颈</li>
 * </ul>
 *
 * <p>生产坑：
 * <ul>
 *   <li>别用 size() 做精确判断（返回值是近似值，高并发下不精确），用 mappingCount() 取 long</li>
 *   <li>computeIfAbsent 内部有锁，不要在 lambda 里做重 IO / 网络调用</li>
 *   <li>key 和 value 都不能为 null（与 HashMap 不同）</li>
 * </ul>
 *
 * @see HashMapSourceAnalysis
 */
public class ConcurrentHashMapSourceAnalysis {

    /**
     * 多线程并发 put，验证数据完整性（无丢失、无覆盖、size 正确）。
     * 对比 HashMap 多线程下可能丢数据/报错。
     */
    public String concurrentPut() throws Exception {
        final int threadCount = 4;
        final int perThread = 1000;
        final int total = threadCount * perThread;

        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int start = t * perThread;
            pool.submit(() -> {
                for (int i = start; i < start + perThread; i++) {
                    map.put(i, "val-" + i);
                }
                latch.countDown();
            });
        }
        latch.await();
        pool.shutdown();

        StringBuilder sb = new StringBuilder();
        sb.append("=== ConcurrentHashMap 并发 put ===\n");
        sb.append(threadCount).append(" 线程 × ").append(perThread)
                .append(" = 预期 ").append(total).append(" 条\n");
        sb.append("实际 size  = ").append(map.size());
        sb.append(map.size() == total ? "  ✓ 无丢失无覆盖" : "  ✗ 数据异常").append('\n');

        // 验证数据可读
        sb.append("get(0)    = ").append(map.get(0)).append('\n');
        sb.append("get(3999) = ").append(map.get(3999)).append('\n');
        return sb.toString();
    }

    /**
     * 原子操作演示：putIfAbsent / computeIfAbsent / merge。
     * 这些方法内部保证原子性，比"先 get 再 put"的 check-then-act 安全。
     *
     * <p>例如统计计数场景，merge 一行搞定并发自增。
     */
    public String atomicOperations() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // putIfAbsent: 仅当 key 不存在时才 put，返回旧值（存在）或 null（新值放入）
        Integer old = map.putIfAbsent("counter", 1);
        Integer blocked = map.putIfAbsent("counter", 100); // 不会覆盖

        // computeIfAbsent: key 不存在 → 执行函数计算值并放入，key 存在 → 直接返回
        Integer computed = map.computeIfAbsent("lazy", k -> k.length());

        // merge: 如果 key 存在则用 remappingFunction 合并，不存在则直接 put
        map.merge("merger", 1, Integer::sum);
        map.merge("merger", 2, Integer::sum);
        map.merge("merger", 3, Integer::sum);

        StringBuilder sb = new StringBuilder();
        sb.append("=== 原子操作演示 ===\n");
        sb.append("putIfAbsent('counter',1)  → 旧值=").append(old).append('\n');
        sb.append("putIfAbsent('counter',100)→ 旧值=").append(blocked)
                .append("  (不会被覆盖)\n");
        sb.append("computeIfAbsent('lazy',String::length) → ").append(computed).append('\n');
        sb.append("merge('merger',1+2+3,Integer::sum)      → ").append(map.get("merger")).append('\n');
        return sb.toString();
    }

    /* ========== 统一入口 ========== */

    public static void main(String[] args) throws Exception {
        ConcurrentHashMapSourceAnalysis demo = new ConcurrentHashMapSourceAnalysis();
        System.out.println(demo.concurrentPut());
        System.out.println(demo.atomicOperations());
    }
}
