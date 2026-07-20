package lan.chaos.java.juc;

import java.util.concurrent.Phaser;

public class PhaserBasicDemo {

    private static final int PARTIES = 3;

    static class Worker implements Runnable {
        private final Phaser phaser;
        private final String name;

        public Worker(Phaser phaser, String name) {
            this.phaser = phaser;
            this.name = name;
            // 注册当前线程
            phaser.register();
        }

        @Override
        public void run() {
            for (int phase = 0; phase < 3; phase++) {
                System.out.println(name + " 完成阶段 " + phase);
                // 到达并等待其他线程
                phaser.arriveAndAwaitAdvance();
            }
            // 完成后注销
            phaser.arriveAndDeregister();
        }
    }

    public static void main(String[] args) {
        Phaser phaser = new Phaser(0); // 初始 0，动态注册

        for (int i = 1; i <= PARTIES; i++) {
            new Thread(new Worker(phaser, "Worker-" + i)).start();
        }
    }
}