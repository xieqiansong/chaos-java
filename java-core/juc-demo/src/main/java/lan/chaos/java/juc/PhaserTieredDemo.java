package lan.chaos.java.juc;

import java.util.concurrent.Phaser;

public class PhaserTieredDemo {

    static class SubTask extends Thread {
        private final Phaser phaser;

        public SubTask(Phaser phaser) {
            this.phaser = phaser;
            phaser.register();
        }

        @Override
        public void run() {
            System.out.println(getName() + " 到达" + phaser.getPhase());
            phaser.arriveAndAwaitAdvance();
        }
    }

    public static void main(String[] args) {
        // 父 Phaser
        Phaser parent = new Phaser(1);

        // 创建多个子 Phaser
        for (int i = 0; i < 3; i++) {
            Phaser child = new Phaser(parent, 0); // 绑定父节点
            for (int j = 0; j < 5; j++) {
                new SubTask(child).start();
            }
        }

        // 父 Phaser 等待所有子 Phaser 完成
        parent.arriveAndAwaitAdvance();
        System.out.println("所有子任务完成");
    }
}