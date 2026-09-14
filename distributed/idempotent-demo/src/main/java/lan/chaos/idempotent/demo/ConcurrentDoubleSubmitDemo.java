package lan.chaos.idempotent.demo;

import lan.chaos.idempotent.common.model.BizOrder;
import lan.chaos.idempotent.common.util.SampleFactory;
import lan.chaos.idempotent.core.RequestIdempotentGuard;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 场景一：请求级并发双发。
 * 模拟前端/网关因超时，对同一 requestId 发出两份写请求。
 * 期望：只有一个请求执行真实副作用，另一个被首检拦截。
 */
public class ConcurrentDoubleSubmitDemo {

    private final RequestIdempotentGuard guard;

    public ConcurrentDoubleSubmitDemo(RequestIdempotentGuard guard) {
        this.guard = guard;
    }

    public String run() {
        String requestId = SampleFactory.newRequestId();
        String bizNo = SampleFactory.newBizNo();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        // 两份相同 requestId 的写请求同时到达
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> guard.execute(requestId, bizNo, () ->
                    BizOrder.builder().bizNo(bizNo).action("CREATE").state("CREATED").build()));
        }
        pool.shutdown();
        try {
            pool.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "requestId=" + requestId + " 副作用执行次数=" + guard.sideEffectCount();
    }
}
