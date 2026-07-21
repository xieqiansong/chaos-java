package lan.chaos.jdk21features.virtualthread;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualThreadDemoTest {

    @Test
    void startVirtualThreadRuns() throws Exception {
        AtomicBoolean ran = new AtomicBoolean();
        Thread vt = Thread.startVirtualThread(() -> ran.set(true));
        vt.join();
        assertTrue(ran.get());
        assertTrue(vt.isVirtual());
    }

    @Test
    void perTaskExecutorRunsAll() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            ArrayList<Future<Integer>> futures = IntStream.range(0, 200)
                    .mapToObj(i -> executor.submit(counter::incrementAndGet))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            for (Future<Integer> f : futures) {
                f.get();
            }
        }
        assertEquals(200, counter.get());
    }
}
