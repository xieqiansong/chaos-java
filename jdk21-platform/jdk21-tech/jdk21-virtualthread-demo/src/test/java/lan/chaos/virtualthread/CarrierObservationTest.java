package lan.chaos.virtualthread;

import lan.chaos.virtualthread.runtime.CarrierObservation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 运行时调度断言：阻塞期间虚拟线程被卸载 →
 * 1) 总耗时远小于「不卸载」时的理论耗时（载体被轮转复用）；
 * 2) 载体线程去重数远小于任务数（仅 ≈CPU 核数）。
 */
class CarrierObservationTest {

    @Test
    void blockingVirtualThreads_areUnmounted_fromCarrier() {
        int n = 256;
        long io = 30;
        CarrierObservation.Observation obs = new CarrierObservation().run(n, io);
        int carriers = Runtime.getRuntime().availableProcessors();
        long serialLike = (long) Math.ceil(n / (double) carriers) * io;

        assertTrue(obs.uniqueCarriers() < obs.virtualCount(),
                "载体线程去重数(" + obs.uniqueCarriers() + ")应小于任务数(" + obs.virtualCount() + ")");
        assertTrue(obs.costMillis() < serialLike,
                "卸载证据：总耗时(" + obs.costMillis() + "ms)应小于不卸载理论耗时(" + serialLike + "ms)");
    }
}
