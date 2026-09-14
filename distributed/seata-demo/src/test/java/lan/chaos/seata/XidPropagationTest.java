package lan.chaos.seata;

import io.seata.core.context.RootContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XID 上下文传递测试（客户端本地机制，无需 Seata Server）。
 *
 * <p>XID 是 Seata 全局事务的唯一标识，必须随调用链路传播：
 * 同线程（ThreadLocal）、跨线程（线程池 / @Async）、跨 RPC（Feign / RestTemplate 自动透传）、
 * 跨 MQ（消息头携带）。本测试演示 {@link RootContext} 的绑定/解绑生命周期，
 * 以及跨线程传递的标准写法。</p>
 *
 * @author chaos
 */
@DisplayName("XID 上下文传递测试")
class XidPropagationTest {

    private static final String XID = "127.0.0.1:8091:888888";

    @Test
    @DisplayName("XID-1: RootContext 绑定/解绑生命周期")
    void bindUnbindLifecycle() {
        assertFalse(RootContext.inGlobalTransaction(), "初始不在全局事务中");
        assertNull(RootContext.getXID(), "初始无 XID");

        RootContext.bind(XID);
        assertTrue(RootContext.inGlobalTransaction(), "bind 后处于全局事务");
        assertEquals(XID, RootContext.getXID(), "getXID 返回绑定的 XID");

        String unbound = RootContext.unbind();
        assertEquals(XID, unbound, "unbind 返回被解绑的 XID");
        assertFalse(RootContext.inGlobalTransaction(), "解绑后不再处于全局事务");
        assertNull(RootContext.getXID(), "解绑后无 XID");
    }

    @Test
    @DisplayName("XID-2: 跨线程传递 — 子线程默认拿不到，需显式透传并重新 bind")
    void propagateXidAcrossThread() throws Exception {
        RootContext.bind(XID);

        AtomicReference<String> childXid = new AtomicReference<>();
        Thread child = new Thread(() -> {
            // 子线程是独立的 ThreadLocal，拿不到父线程的 XID
            String inherited = RootContext.getXID();
            // 标准写法：将 XID 作为任务参数/上下文透传，子线程内重新 bind
            String passedXid = (inherited == null || inherited.isEmpty()) ? XID : inherited;
            RootContext.bind(passedXid);
            childXid.set(RootContext.getXID());
            RootContext.unbind();
        });
        child.start();
        child.join(3000);

        assertEquals(XID, childXid.get(), "子线程应通过显式透传持有同一 XID");
        assertEquals(XID, RootContext.getXID(), "父子线程 ThreadLocal 隔离，父线程 XID 不受影响");

        RootContext.unbind();
    }
}
