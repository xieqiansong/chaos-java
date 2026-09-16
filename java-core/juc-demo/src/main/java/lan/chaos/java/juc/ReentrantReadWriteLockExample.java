package lan.chaos.java.juc;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 下面是一个详细展示 ReentrantReadWriteLock 用法的代码示例，包含缓存系统实现、锁降级演示和性能测试
 */
public class ReentrantReadWriteLockExample {
    // 性能测试方法
    private static void runPerformanceTest() throws InterruptedException {
        final ReadWriteCache<String, String> cache = new ReadWriteCache<>();
        final int threadCount = 20;
        final ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        final CyclicBarrier barrier = new CyclicBarrier(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);

        final Random random = new Random();
        final String[] keys = {"A", "B", "C", "D", "E"};

        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            pool.execute(() -> {
                try {
                    barrier.await(); // 同时开始

                    // 95% 读操作，5% 写操作
                    for (int j = 0; j < 100; j++) {
                        if (random.nextInt(100) < 95) {
                            cache.get(keys[random.nextInt(keys.length)]);
                        } else {
                            String key = keys[random.nextInt(keys.length)];
                            cache.put(key, "NewData-" + System.nanoTime());
                        }
                        Thread.sleep(random.nextInt(5)); // 模拟处理时间
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long duration = System.currentTimeMillis() - start;
        pool.shutdown();

        System.out.println("\n====== 性能测试结果 ======");
        System.out.println("线程数: " + threadCount);
        System.out.println("总耗时: " + duration + "ms");
        cache.printStats();
    }

    public static void main(String[] args) throws InterruptedException {
        // 1. 创建缓存实例
        ReadWriteCache<String, String> cache = new ReadWriteCache<>();

        // 2. 初始化一些数据
        cache.put("Config", "InitialValue");
        cache.put("Metadata", "InitialData");

        // 3. 模拟并发读取
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++) {
            executor.execute(() -> {
                for (int j = 0; j < 5; j++) {
                    cache.get("Config");
                    cache.get("Metadata");
                }
            });
        }

        // 4. 模拟并发写入
        executor.execute(() -> cache.put("Config", "UpdatedValue"));
        executor.execute(() -> cache.put("Metadata", "UpdatedData"));
        executor.execute(() -> cache.put("LogLevel", "DEBUG"));

        // 5. 锁降级演示
        executor.execute(() -> {
            String result = cache.getAndUpdate("Config", "FinalValue");
            System.out.println(Thread.currentThread().getName() + " | " + result);
        });

        // 6. 关闭线程池并输出统计
        Thread.sleep(1000);
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        cache.printStats();

        // 7. 运行性能测试
        runPerformanceTest();
    }

    // 缓存类实现（线程安全）
    static class ReadWriteCache<K, V> {
        private final Map<K, V> cacheMap = new HashMap<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock(true); // true表示公平锁
        private final Lock readLock = lock.readLock();
        private final Lock writeLock = lock.writeLock();
        private int hitCount;
        private int missCount;

        // 读操作（高并发访问）
        public V get(K key) {
            readLock.lock();
            try {
                V value = cacheMap.get(key);
                if (value != null) {
                    hitCount++;
                    System.out.println(Thread.currentThread().getName() + " 读取命中: " + key);
                    return value;
                }
            } finally {
                readLock.unlock();
            }

            // 缓存未命中时的处理（锁升级处理）
            missCount++;
            return loadFromDataSource(key);
        }

        // 写操作（独占访问）
        public void put(K key, V value) {
            writeLock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + " 正在写入: " + key);
                Thread.sleep(10); // 模拟写操作耗时
                cacheMap.put(key, value);
                System.out.println(Thread.currentThread().getName() + " 写入完成: " + key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                writeLock.unlock();
            }
        }

        // 加载数据（模拟数据库查询）
        private V loadFromDataSource(K key) {
            System.out.println(Thread.currentThread().getName() + " 缓存未命中: " + key);
            // 创建新值前需要获取写锁
            writeLock.lock();
            try {
                // 双检锁（Double-Check）
                V value = cacheMap.get(key);
                if (value == null) {
                    // 模拟耗时数据库查询
                    Thread.sleep(50);
                    // 制造数据（实际应用中这里应该查询数据库）
                    @SuppressWarnings("unchecked")
                    V newValue = (V) ("Data-of-" + key);
                    cacheMap.put(key, newValue);
                    System.out.println(Thread.currentThread().getName() + " 加载到缓存: " + key);
                    return newValue;
                }
                return value;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                writeLock.unlock();
            }
        }

        // 锁降级演示（写锁→读锁降级）
        public String getAndUpdate(K key, V newValue) {
            writeLock.lock();
            try {
                // 1. 先获取当前值
                V current = cacheMap.get(key);

                // 2. 模拟一些验证操作
                System.out.println(Thread.currentThread().getName() + " 正在验证数据...");
                Thread.sleep(20);

                // 3. 锁降级：在写锁保护下获取读锁
                readLock.lock();
                System.out.println(Thread.currentThread().getName() + " 获取到读锁（降级）");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                writeLock.unlock(); // 释放写锁，降级为读锁
            }

            try {
                // 4. 在读取状态下更新数据（演示目的，实际应用中不推荐在降级后更新）
                cacheMap.put(key, newValue);
                return "操作完成: " + key + "=" + cacheMap.get(key);
            } finally {
                readLock.unlock();
            }
        }

        public void printStats() {
            System.out.println("\n====== 缓存统计 ======");
            System.out.println("命中次数: " + hitCount);
            System.out.println("未命中次数: " + missCount);
            System.out.println("缓存条目: " + cacheMap.size());
            System.out.println("====================\n");
        }
    }
}
