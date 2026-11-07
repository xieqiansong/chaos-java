package lan.chaos.finance.batch;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.store.AccountStore;
import lan.chaos.finance.common.store.SubjectStore;
import lan.chaos.finance.ledger.LedgerSamples;
import lan.chaos.finance.ledger.ReconcileLedger;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 日终分片、断点续跑「不重不丢」断言测试。 */
class DayEndJobTest {

    private final Money zero = Money.of("0.00", Currency.CNY);

    private ReconcileLedger consistentReconcile() {
        AccountStore accounts = new AccountStore();
        SubjectStore subjects = new SubjectStore();
        subjects.save(LedgerSamples.sampleSubject());
        accounts.save(LedgerSamples.sampleAccount("A", "alice", "1001"));
        return new ReconcileLedger(accounts, subjects);
    }

    @Test
    void 全量跑通且分片数正确() {
        CheckpointStore cp = new CheckpointStore();
        List<String> handled = new ArrayList<>();
        DayEndJob.DayEndReport report = new DayEndJob().run(LocalDate.of(2026, 11, 7),
                consistentReconcile(), DayEndJob.sampleItems(), cp, zero,
                w -> handled.add(w.getId()), null);

        assertThat(report.isLedgerConsistent()).isTrue();
        assertThat(report.getBatchResult().getProcessed()).isEqualTo(5);
        assertThat(report.getBatchResult().getShards()).isEqualTo(3);
        assertThat(report.getTotalAmount().compareTo(Money.of("1500.00", Currency.CNY))).isEqualTo(0);
        assertThat(handled).containsExactly("D1", "D2", "D3", "D4", "D5");
    }

    @Test
    void 中途失败后续跑不重不丢() {
        CheckpointStore cp = new CheckpointStore();
        List<String> handled = new ArrayList<>();
        DayEndJob job = new DayEndJob();

        // 第一轮：在 D3 处宕机，D1/D2 已完成
        assertThatThrownBy(() -> job.run(LocalDate.now(), consistentReconcile(),
                DayEndJob.sampleItems(), cp, zero, w -> handled.add(w.getId()), "D3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("D3");
        assertThat(handled).containsExactly("D1", "D2");
        assertThat(cp.doneCount()).isEqualTo(2);

        // 第二轮：断点续跑，跳过 D1/D2，只补 D3/D4/D5
        DayEndJob.DayEndReport report = job.run(LocalDate.now(), consistentReconcile(),
                DayEndJob.sampleItems(), cp, zero, w -> handled.add(w.getId()), null);
        assertThat(report.getBatchResult().getProcessed()).isEqualTo(3);
        assertThat(report.getBatchResult().getSkippedDuplicated()).isEqualTo(2);
        assertThat(handled).containsExactly("D1", "D2", "D3", "D4", "D5"); // 无重复
        assertThat(cp.doneCount()).isEqualTo(5);
    }

    @Test
    void 总分不一致时日终直接失败() {
        AccountStore accounts = new AccountStore();
        SubjectStore subjects = new SubjectStore();
        subjects.save(LedgerSamples.sampleSubject());
        lan.chaos.finance.common.model.Account a =
                LedgerSamples.sampleAccount("A", "alice", "1001");
        a.setBalance(Money.of("1.00", Currency.CNY)); // 篡改分户账，破坏勾稽
        accounts.save(a);

        assertThatThrownBy(() -> new DayEndJob().run(LocalDate.now(),
                new ReconcileLedger(accounts, subjects), DayEndJob.sampleItems(),
                new CheckpointStore(), zero, w -> {
                }, null))
                .isInstanceOf(lan.chaos.finance.common.exception.ReconcileException.class);
    }
}
