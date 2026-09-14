package lan.chaos.filterasync.service;

import lan.chaos.filterasync.model.StatusReport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 下游「入库」模拟：本压测刻意保持极轻（仅计数），聚焦「链路」CPU 成本，
 * 与入库/业务耗时无关（压测目标：度量绕过 MVC 能省多少链路开销）。
 *
 * <p>真实场景中这里会做 Bitmap 置位 / 入批队列 / 落 Redis，由独立线程池承接，
 * 因而 HTTP 线程在 {@code submit} 后即可释放——异步层与 IO 层职责分离。
 */
@Component
public class ReportSink {

    private final Executor executor;
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public ReportSink(@Qualifier("reportAsyncExecutor") Executor executor) {
        this.executor = executor;
    }

    /** 异步提交：交给隔离线程池，立即返回（fire-and-forget）。 */
    public void submit(StatusReport report, String clientIp) {
        java.util.concurrent.CompletableFuture
                .runAsync(() -> {
                    // 下游处理：本压测仅计数；真实场景为 Bitmap 置位 / 入批队列等
                    accepted.incrementAndGet();
                }, executor)
                .whenComplete((v, ex) -> {
                    if (ex != null) {
                        failed.incrementAndGet();
                    }
                });
    }

    public long accepted() {
        return accepted.get();
    }

    public long failed() {
        return failed.get();
    }
}
