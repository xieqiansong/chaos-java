package lan.chaos.virtualthread.runner;

import lan.chaos.virtualthread.common.constant.Scenario;
import lan.chaos.virtualthread.pinning.PinningCompare;
import lan.chaos.virtualthread.runtime.CarrierObservation;
import lan.chaos.virtualthread.structured.StructuredConcurrency;
import lan.chaos.virtualthread.threadlocal.ThreadLocalSemantics;
import lan.chaos.virtualthread.throughput.ThroughputCompare;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分节主线：按场景依次打印「输入 → 输出」。
 * 运行方式：mvn spring-boot:run（或直接运行 VirtualThreadApplication.main）。
 */
@Component
public class DemoRunner implements ApplicationRunner {

    private static final Map<Scenario, Runnable> SCENARIOS = new LinkedHashMap<>();

    static {
        SCENARIOS.put(Scenario.THROUGHPUT, new ThroughputCompare()::demo);
        SCENARIOS.put(Scenario.RUNTIME, new CarrierObservation()::demo);
        SCENARIOS.put(Scenario.PINNING, new PinningCompare()::demo);
        SCENARIOS.put(Scenario.STRUCTURED, new StructuredConcurrency()::demo);
        SCENARIOS.put(Scenario.THREADLOCAL, new ThreadLocalSemantics()::demo);
    }

    @Override
    public void run(ApplicationArguments args) {
        SCENARIOS.forEach((scenario, demo) -> {
            System.out.println();
            System.out.println("========================================");
            System.out.println("场景: " + scenario.name() + " —— " + scenario.desc());
            System.out.println("----------------------------------------");
            demo.run();
        });
    }
}
