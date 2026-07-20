package lan.chaos.java.juc.blocking.queue;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayQueueExample {
    public static class DelayedElement implements Delayed {

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(10, TimeUnit.SECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return 0;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        DelayQueue queue = new DelayQueue();
        Delayed element1 = new DelayedElement();
        queue.put(element1);
        Delayed element2 = queue.take();
        System.out.println(element2);
    }
}