package lan.chaos.virtualthread;

import lan.chaos.virtualthread.structured.StructuredConcurrency;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构化并发断言：成功并行耗时≈单个任务、任一失败整体失败、超时到点抛 TimeoutException。
 */
class StructuredConcurrencyTest {

    private final StructuredConcurrency conc = new StructuredConcurrency();

    @Test
    void parallelSubtasks_runConcurrently() throws Exception {
        long io = 100;
        long start = System.nanoTime();
        long sum = conc.runParallelSuccess(io);
        long cost = (System.nanoTime() - start) / 1_000_000;

        assertEquals(2 * io, sum);
        assertTrue(cost < 2 * io, "并行总耗时(" + cost + "ms)应小于串行耗时(" + 2 * io + "ms)");
    }

    @Test
    void failureOfOneSubtask_failsWholeScope() {
        assertThrows(ExecutionException.class, () -> conc.runFailurePropagation());
    }

    @Test
    void joinUntilDeadline_exceedsTimeout() {
        assertThrows(TimeoutException.class, () -> conc.runTimeout());
    }
}
