package lan.chaos.finance.payment;

import lan.chaos.finance.common.constant.OrderStatus;
import lan.chaos.finance.common.exception.IllegalStateTransitionException;
import lan.chaos.finance.common.store.PaymentOrderStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 状态机 + 幂等断言测试（纯内存，零外部依赖）。
 * 并发用例直接回答对话的防御性思维第一问："被并发调用两次会怎样？"
 */
class PaymentTest {

    private PaymentOrderStore store;
    private TradeStateMachine stateMachine;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        store = new PaymentOrderStore();
        stateMachine = new TradeStateMachine();
        service = new PaymentService(store, stateMachine);
    }

    @Test
    void 首次支付完成扣款且状态到达PAID() {
        PayResult result = service.pay("B1", PaymentSamples.sampleAmount());
        assertThat(result.isDuplicated()).isFalse();
        assertThat(result.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getOrder().getVersion()).isEqualTo(2); // INIT→PAYING→PAID
        assertThat(service.getChargeCount()).isEqualTo(1);
    }

    @Test
    void 重复支付幂等命中不再扣款() {
        PayResult first = service.pay("B1", PaymentSamples.sampleAmount());
        PayResult again = service.pay("B1", PaymentSamples.sampleAmount());
        assertThat(first.isDuplicated()).isFalse();
        assertThat(again.isDuplicated()).isTrue();
        assertThat(again.getOrder()).isSameAs(first.getOrder()); // 结果一致：同一订单
        assertThat(again.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(service.getChargeCount()).isEqualTo(1);
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    void 并发重复支付只扣一次() throws Exception {
        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<PayResult>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return service.pay("B-CONCURRENT", PaymentSamples.sampleAmount());
                }));
            }
            start.countDown();
            int realCharges = 0;
            for (Future<PayResult> f : futures) {
                PayResult r = f.get(5, TimeUnit.SECONDS);
                assertThat(r.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);
                if (!r.isDuplicated()) {
                    realCharges++;
                }
            }
            assertThat(realCharges).isEqualTo(1);
            assertThat(service.getChargeCount()).isEqualTo(1);
            assertThat(service.getChargeLog()).hasSize(1);
            assertThat(store.size()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void 非法状态跃迁被拒绝() {
        assertThatThrownBy(() -> service.refund("B-NOT-EXIST"))
                .isInstanceOf(IllegalArgumentException.class);

        // 待支付直接退款：非法（未支付就退款 = 白给钱）
        service.pay("B2", PaymentSamples.sampleAmount());
        // 先造一个 INIT 订单来直接测状态机
        PaymentOrderStore freshStore = new PaymentOrderStore();
        TradeStateMachine sm = new TradeStateMachine();
        lan.chaos.finance.common.model.TradeOrder initOrder =
                freshStore.getOrCreate("B3", () -> PaymentSamples.newOrder("B3", PaymentSamples.sampleAmount()));
        assertThatThrownBy(() -> sm.transition(initOrder, OrderStatus.REFUNDED))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining("非法状态跃迁");
        assertThat(initOrder.getStatus()).isEqualTo(OrderStatus.INIT); // 状态未被污染
    }

    @Test
    void 退款只能从PAID发起() {
        service.pay("B4", PaymentSamples.sampleAmount());
        assertThat(service.refund("B4").getStatus()).isEqualTo(OrderStatus.REFUNDED);
        // 二次退款：REFUNDED 是终态，再退应被状态机拒绝
        assertThatThrownBy(() -> service.refund("B4"))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void 中途失败落到FAILED而非悬挂PAYING() {
        PaymentService failing = new PaymentService(store, stateMachine, order -> {
            throw new IllegalStateException("渠道超时");
        });
        assertThatThrownBy(() -> failing.pay("B5", PaymentSamples.sampleAmount()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(store.findByBizNo("B5").getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(failing.getChargeCount()).isEqualTo(0); // 失败未记账
    }

    @Test
    void 状态机规则表可观察() {
        assertThat(stateMachine.allowedTargets(OrderStatus.INIT))
                .containsExactlyInAnyOrder(OrderStatus.PAYING, OrderStatus.FAILED);
        assertThat(stateMachine.allowedTargets(OrderStatus.PAID))
                .containsExactly(OrderStatus.REFUNDED);
        assertThat(stateMachine.allowedTargets(OrderStatus.REFUNDED)).isEmpty();
    }
}
