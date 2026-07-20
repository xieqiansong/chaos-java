package lan.chaos.java.juc;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockDeadlockDemo {

    private static final Lock lockA = new ReentrantLock();
    private static final Lock lockB = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            lockA.lock();
            try {
                System.out.println("Thread-1 持有 lockA");
                Thread.sleep(100); // 确保另一个线程拿到 lockB
                lockB.lock();
                try {
                    System.out.println("Thread-1 持有 lockA 和 lockB");
                } finally {
                    lockB.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lockA.unlock();
            }
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            lockB.lock();
            try {
                System.out.println("Thread-2 持有 lockB");
                Thread.sleep(100);
                lockA.lock();
                try {
                    System.out.println("Thread-2 持有 lockB 和 lockA");
                } finally {
                    lockA.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lockB.unlock();
            }
        }, "Thread-2");

        t1.start();
        t2.start();

        // 让死锁发生
        Thread.sleep(500);

        // 使用 ThreadMXBean 检测
        detectDeadlock();
    }

    private static void detectDeadlock() {
        java.lang.management.ThreadMXBean threadBean =
                java.lang.management.ManagementFactory.getThreadMXBean();

        System.out.println("\n=== 使用 findMonitorDeadlockedThreads() ===");
        long[] monitorDeadlocked = threadBean.findDeadlockedThreads();
        if (monitorDeadlocked != null) {
            System.out.println("发现监视器死锁线程数：" + monitorDeadlocked.length);
        } else {
            System.out.println("未发现监视器死锁。");
        }

        System.out.println("\n=== 使用 findDeadlockedThreads() ===");
        long[] deadlocked = threadBean.findDeadlockedThreads();
        if (deadlocked != null) {
            System.out.println("发现死锁线程数：" + deadlocked.length);
            for (long id : deadlocked) {
                java.lang.management.ThreadInfo info =
                        threadBean.getThreadInfo(id, Integer.MAX_VALUE);
                System.out.println("死锁线程: " + info.getThreadName()
                        + " (ID=" + id + ")");
                System.out.println("等待的锁: " + info.getLockInfo());
                System.out.println("持有的锁: ");
                for (java.lang.management.MonitorInfo mi : info.getLockedMonitors()) {
                    System.out.println("  - " + mi);
                }
                // 注意：Lock 的持有信息不会出现在 getLockedMonitors() 中，
                // 需要通过 getLockedSynchronizers() 查看
                System.out.println("持有的同步器(Lock): ");
                for (java.lang.management.LockInfo li : info.getLockedSynchronizers()) {
                    System.out.println("  - " + li);
                }
                System.out.println("---------------------------");
            }
        } else {
            System.out.println("未发现任何死锁。");
        }
    }
}