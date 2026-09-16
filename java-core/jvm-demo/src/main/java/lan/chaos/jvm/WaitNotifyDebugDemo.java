package lan.chaos.jvm;

public class WaitNotifyDebugDemo {

    // 共享锁对象
    private static final Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {

        // 等待线程
        Thread waiter = new Thread(() -> {
            synchronized (LOCK) {
                try {
                    System.out.println("waiter: 准备进入 wait");
                    LOCK.wait(); // ✅ 断点 1
                    System.out.println("waiter: 被唤醒继续执行");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Waiter-Thread");

        // 通知线程
        Thread notifier = new Thread(() -> {
            synchronized (LOCK) {
                System.out.println("notifier: 准备 notify");
                LOCK.notify(); // ✅ 断点 2
                System.out.println("notifier: 已发送 notify");
            }
        }, "Notifier-Thread");

        waiter.start();

        // 确保 waiter 先拿到锁
        Thread.sleep(100);

        notifier.start();
    }
}
