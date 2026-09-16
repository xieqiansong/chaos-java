package lan.chaos.java.juc;

import java.util.concurrent.Phaser;

public class PhaserDynamicDemo {

    static class Task implements Runnable {
        private final Phaser phaser;

        public Task(Phaser phaser) {
            this.phaser = phaser;
            phaser.register(); // 动态加入
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " 开始执行，当前阶段：" + phaser.getPhase());
            phaser.arriveAndAwaitAdvance();
            System.out.println(Thread.currentThread().getName() + " 进入下一阶段，当前阶段：" + phaser.getPhase());
            phaser.arriveAndDeregister();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Phaser phaser = new Phaser(0);
        System.out.println("初始阶段：" + phaser.getPhase());

        // 模拟运行时不断有新任务加入
        for (int i = 0; i < 5; i++) {
            new Thread(new Task(phaser), "Task-" + i).start();
            Thread.sleep(100);
        }
    }
}