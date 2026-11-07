package lan.chaos.finance.settlement;

import lan.chaos.finance.common.constant.ClearingStatus;
import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.exception.ClearingException;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.Position;
import lan.chaos.finance.common.store.ClearingBillStore;
import lan.chaos.finance.common.store.PositionStore;
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

/** TCC 三阶段、幂等、空回滚、防悬挂、资金守恒与并发断言测试。 */
class TccClearingTest {

    private PositionStore positions;
    private TccClearing tcc;
    private Money amount;

    @BeforeEach
    void setUp() {
        positions = new PositionStore();
        positions.save(SettlementSamples.samplePosition("A", "1000.00"));
        positions.save(SettlementSamples.samplePosition("B", "1000.00"));
        tcc = new TccClearing(positions, new ClearingBillStore());
        amount = Money.of("100.00", Currency.CNY);
    }

    @Test
    void Try冻结资金且守恒() {
        Position a = positions.get("A");
        Money before = a.total();
        assertThat(tcc.tryFreeze("C1", "A", "B", amount).getStatus()).isEqualTo(ClearingStatus.TRY);
        assertThat(a.getAvailable().compareTo(Money.of("900.00", Currency.CNY))).isEqualTo(0);
        assertThat(a.getFrozen().compareTo(amount)).isEqualTo(0);
        assertThat(a.total().compareTo(before)).isEqualTo(0); // 资金守恒
    }

    @Test
    void Confirm实际划拨且幂等() {
        tcc.tryFreeze("C1", "A", "B", amount);
        assertThat(tcc.confirm("C1").getStatus()).isEqualTo(ClearingStatus.CONFIRMED);
        assertThat(tcc.confirm("C1").getStatus()).isEqualTo(ClearingStatus.CONFIRMED); // 幂等
        assertThat(positions.get("A").getAvailable().compareTo(Money.of("900.00", Currency.CNY))).isEqualTo(0);
        assertThat(positions.get("B").getAvailable().compareTo(Money.of("1100.00", Currency.CNY))).isEqualTo(0);
        assertThat(positions.get("A").getFrozen().isZero()).isTrue();
    }

    @Test
    void Cancel解冻归还且守恒() {
        Position a = positions.get("A");
        Money before = a.total();
        tcc.tryFreeze("C1", "A", "B", amount);
        assertThat(tcc.cancel("C1").getStatus()).isEqualTo(ClearingStatus.CANCELED);
        assertThat(a.getAvailable().compareTo(before)).isEqualTo(0);
        assertThat(a.getFrozen().isZero()).isTrue();
        // Cancel 后不允许再 Confirm（防悬挂/状态倒退）
        assertThatThrownBy(() -> tcc.confirm("C1")).isInstanceOf(ClearingException.class);
    }

    @Test
    void 空回滚标记拦截迟到的Try() {
        // Cancel 先于 Try 到达（网络乱序）
        assertThat(tcc.cancel("C-X")).isNull();
        assertThatThrownBy(() -> tcc.tryFreeze("C-X", "A", "B", amount))
                .isInstanceOf(ClearingException.class)
                .hasMessageContaining("防悬挂");
        // 头寸未被误冻结
        assertThat(positions.get("A").getFrozen().isZero()).isTrue();
    }

    @Test
    void 头寸不足冻结被拒() {
        Money huge = Money.of("99999.00", Currency.CNY);
        assertThatThrownBy(() -> tcc.tryFreeze("C-BIG", "A", "B", huge))
                .isInstanceOf(ClearingException.class)
                .hasMessageContaining("可用头寸不足");
        assertThat(positions.get("A").getAvailable().compareTo(Money.of("1000.00", Currency.CNY))).isEqualTo(0);
    }

    /**
     * 并发回归：幂等检查必须在临界区内。
     * 修复前两个线程会同时看到 existing==null，各自冻结一次而只落一张单 →
     * 冻结 200 / 单据只释放 100，剩余 100 永久挂账。修复后必须只冻结一次。
     */
    @Test
    void 并发重复Try只冻结一次且Confirm后无挂账() throws Exception {
        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ClearingStatus>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return tcc.tryFreeze("C-CONCURRENT", "A", "B", amount).getStatus();
                }));
            }
            start.countDown();
            for (Future<ClearingStatus> f : futures) {
                assertThat(f.get(5, TimeUnit.SECONDS)).isEqualTo(ClearingStatus.TRY);
            }
        } finally {
            pool.shutdownNow();
        }

        Position a = positions.get("A");
        assertThat(a.getFrozen().compareTo(amount)).isEqualTo(0);          // 只冻结一次
        assertThat(a.getAvailable().compareTo(Money.of("900.00", Currency.CNY))).isEqualTo(0);
        assertThat(a.total().compareTo(Money.of("1000.00", Currency.CNY))).isEqualTo(0);

        // Confirm 后冻结必须完全释放，不能有永久挂账
        tcc.confirm("C-CONCURRENT");
        assertThat(a.getFrozen().isZero()).isTrue();
        assertThat(a.getAvailable().compareTo(Money.of("900.00", Currency.CNY))).isEqualTo(0);
        assertThat(positions.get("B").getAvailable()
                .compareTo(Money.of("1100.00", Currency.CNY))).isEqualTo(0);
    }

    @Test
    void 并发Confirm幂等不重复入账() throws Exception {
        tcc.tryFreeze("C-CFM", "A", "B", amount);
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ClearingStatus>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return tcc.confirm("C-CFM").getStatus();
                }));
            }
            start.countDown();
            for (Future<ClearingStatus> f : futures) {
                assertThat(f.get(5, TimeUnit.SECONDS)).isEqualTo(ClearingStatus.CONFIRMED);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(positions.get("B").getAvailable()
                .compareTo(Money.of("1100.00", Currency.CNY))).isEqualTo(0); // 只入账一次
    }
}
