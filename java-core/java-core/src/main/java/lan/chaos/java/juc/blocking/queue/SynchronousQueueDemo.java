package lan.chaos.java.juc.blocking.queue;

import java.util.concurrent.SynchronousQueue;

public class SynchronousQueueDemo {

    public static void main(String[] args) throws InterruptedException {
        SynchronousQueue queue = new SynchronousQueue();
        queue.put("1");
        System.out.println(queue.take());
    }
}
