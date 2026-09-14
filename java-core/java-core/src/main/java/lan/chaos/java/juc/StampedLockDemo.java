package lan.chaos.java.juc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

public class StampedLockDemo {
    private final StampedLock lock = new StampedLock();
    private int sharedValue = 0;

    public static void main(String[] args) throws InterruptedException {
        StampedLockDemo example = new StampedLockDemo();
        ExecutorService executor = Executors.newFixedThreadPool(3);

        {
            // 写任务（10次递增）
            executor.submit(() -> {
                for (int i = 0; i < 10; i++) {
                    example.increment();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }
            });

            // 悲观读任务（每200ms读一次）
            executor.submit(() -> {
                for (int i = 0; i < 5; i++) {
                    example.getValue();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                }
            });

            // 乐观读任务（每50ms尝试读一次）
            executor.submit(() -> {
                for (int i = 0; i < 20; i++) {
                    example.optimisticRead();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                }
            });

        }
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);
    }

    // 写操作：增加共享值
    public void increment() {
        // 获取写锁
        long stamp = lock.writeLock();
        try {
            System.out.println(Thread.currentThread().getName() + " 获取写锁");
            sharedValue++;
            System.out.println("值更新为: " + sharedValue);
        } finally {
            // 释放写锁
            lock.unlockWrite(stamp);
        }
    }

    // 悲观读操作：获取值
    public int getValue() {
        // 获取读锁
        long stamp = lock.readLock();
        try {
            System.out.println(Thread.currentThread().getName() + " 获取悲观读锁");
            return sharedValue;
        } finally {
            // 释放读锁
            lock.unlockRead(stamp);
        }
    }

    // 乐观读操作：尝试读取
    public void optimisticRead() {
        // 获取乐观读戳
        long stamp = lock.tryOptimisticRead();
        int value = sharedValue;

        // 检查读过程中是否有写操作发生
        if (!lock.validate(stamp)) {
            System.out.println(Thread.currentThread().getName() + " 乐观读失效，转为悲观读");
            // 升级为悲观读锁
            stamp = lock.readLock();
            try {
                value = sharedValue;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        System.out.println(Thread.currentThread().getName() + " 读取值: " + value);
    }
}
