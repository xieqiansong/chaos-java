package lan.chaos.java.juc;

import java.util.concurrent.Phaser;

/**
 * 在 Java 中，Phaser 是 JUC 包提供的强大同步工具，用于控制多线程分阶段执行。它允许线程在某个阶段（phase）同步并等待其他线程，且支持动态增减参与者数量。下面是一个完整的代码示例
 */
public class PhaserExample {

    public static void main(String[] args) {
        // 创建 Phaser（初始参与者数 = 3）
        Phaser phaser = new Phaser(3) {
            // 可选：覆盖 onAdvance() 方法定义阶段切换时的行为
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                System.out.println("\n=== 阶段 " + phase + " 完成 ===");
                return registeredParties == 0; // 返回 true 会终止 Phaser
            }
        };

        // 创建并启动3个任务线程
        for (int i = 1; i <= 3; i++) {
            new Thread(new Task(phaser, "Worker-" + i)).start();
        }
    }

    static class Task implements Runnable {
        private final Phaser phaser;
        private final String name;

        public Task(Phaser phaser, String name) {
            this.phaser = phaser;
            this.name = name;
        }

        @Override
        public void run() {
            try {
                // 阶段1任务
                System.out.printf("%s 执行阶段 %d 任务\n", name, phaser.getPhase());
                Thread.sleep(500);
                phaser.arriveAndAwaitAdvance(); // 等待所有线程完成阶段1

                // 阶段2任务（动态新增一个线程）
                if ("Worker-1".equals(name)) {
                    System.out.println("\n--- 新增 Worker-4 ---");
                    phaser.register(); // 注册新参与者
                    new Thread(new Task(phaser, "Worker-4")).start();
                }

                System.out.printf("%s 执行阶段 %d 任务\n", name, phaser.getPhase());
                Thread.sleep(1000);
                phaser.arriveAndAwaitAdvance(); // 等待阶段2

                // 阶段3任务（Worker-1 提前退出）
                if ("Worker-1".equals(name)) {
                    phaser.arriveAndDeregister(); // 完成任务并注销
                    return;
                }

                System.out.printf("%s 执行阶段 %d 任务\n", name, phaser.getPhase());
                Thread.sleep(800);
                phaser.arriveAndAwaitAdvance(); // 等待阶段3

                // 所有阶段完成后退出
                phaser.arriveAndDeregister();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
