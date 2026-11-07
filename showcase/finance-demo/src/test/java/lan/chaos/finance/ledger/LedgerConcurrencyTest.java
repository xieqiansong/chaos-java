package lan.chaos.finance.ledger;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Account;
import lan.chaos.finance.common.model.Direction;
import lan.chaos.finance.common.model.JournalEntry;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.Subject;
import lan.chaos.finance.common.model.Voucher;
import lan.chaos.finance.common.store.AccountStore;
import lan.chaos.finance.common.store.SubjectStore;
import lan.chaos.finance.common.store.VoucherStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 记账并发正确性回归测试（对应对话 Java 映射表「同一账户不被并发透支/重复扣减」）。
 *
 * <p>修复前 {@code DoubleEntryEngine.post} 的 {@code getBalance().add(delta)} 是
 * read-modify-write，多线程并发记账会丢失更新；且勾稽线程可能读到"分户已改、总账未改"
 * 的撕裂状态。修复后整笔凭证在临界区内原子完成，两者共用同一把锁。
 */
class LedgerConcurrencyTest {

    private AccountStore accountStore;
    private SubjectStore subjectStore;
    private VoucherStore voucherStore;
    private DoubleEntryEngine engine;
    private ReconcileLedger reconcile;

    @BeforeEach
    void setUp() {
        accountStore = new AccountStore();
        subjectStore = new SubjectStore();
        voucherStore = new VoucherStore();
        Subject subject = LedgerSamples.sampleSubject();
        subjectStore.save(subject);
        accountStore.save(LedgerSamples.sampleAccount("A", "alice", subject.getCode()));
        accountStore.save(LedgerSamples.sampleAccount("B", "bob", subject.getCode()));
        engine = new DoubleEntryEngine(accountStore, subjectStore, voucherStore);
        reconcile = new ReconcileLedger(accountStore, subjectStore);
    }

    /** 构造一笔带唯一 id 的 A→B 转账凭证：A 贷(CREDIT) 减少、B 借(DEBIT) 增加。 */
    private Voucher transfer(int seq, Money amount) {
        Voucher v = new Voucher();
        v.setId("V-" + seq);
        v.setBizNo("BIZ-" + seq);
        v.setType("TRANSFER");

        JournalEntry creditA = new JournalEntry();
        creditA.setId("V-" + seq + "-E1");
        creditA.setAccountId("A");
        creditA.setSubjectCode("1001");
        creditA.setDirection(Direction.CREDIT);
        creditA.setAmount(amount);
        v.addEntry(creditA);

        JournalEntry debitB = new JournalEntry();
        debitB.setId("V-" + seq + "-E2");
        debitB.setAccountId("B");
        debitB.setSubjectCode("1001");
        debitB.setDirection(Direction.DEBIT);
        debitB.setAmount(amount);
        v.addEntry(debitB);
        return v;
    }

    @Test
    void 热点账户并发记账不丢失更新() throws Exception {
        int threads = 64;
        Money one = Money.of("1.00", Currency.CNY);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                final int seq = i;
                futures.add(pool.submit(() -> {
                    start.await();
                    engine.post(transfer(seq, one));
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(5, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        // 修复前会 < 64（丢失更新）；修复后必须精确等于 64 次累加
        Account a = accountStore.get("A");
        Account b = accountStore.get("B");
        assertThat(a.getBalance().compareTo(Money.of("-64.00", Currency.CNY))).isEqualTo(0);
        assertThat(b.getBalance().compareTo(Money.of("64.00", Currency.CNY))).isEqualTo(0);
        assertThat(a.getVersion()).isEqualTo(64); // 每个账户被更新 64 次
        reconcile.assertConsistent();            // 总分仍勾稽一致
    }

    @Test
    void 并发记账期间总分勾稽不读到撕裂状态() throws Exception {
        int rounds = 200;
        Money one = Money.of("1.00", Currency.CNY);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);

        Thread writer = new Thread(() -> {
            try {
                start.await();
                for (int i = 0; i < rounds; i++) {
                    engine.post(transfer(i, one));
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        });
        Thread checker = new Thread(() -> {
            try {
                start.await();
                for (int i = 0; i < rounds; i++) {
                    // 与 post 共用 accountStore 锁 → 每次读到的都是完整快照
                    reconcile.assertConsistent();
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        });

        writer.start();
        checker.start();
        start.countDown();
        writer.join(5000);
        checker.join(5000);

        assertThat(failure.get())
                .as("勾稽线程不应观察到撕裂状态（尤其不能是 ReconcileException）")
                .isNull();
        reconcile.assertConsistent();
    }
}
