package lan.chaos.finance.common.runner;

import lan.chaos.finance.audit.AuditLogger;
import lan.chaos.finance.audit.MaskUtils;
import lan.chaos.finance.batch.CheckpointStore;
import lan.chaos.finance.batch.DayEndJob;
import lan.chaos.finance.batch.WorkItem;
import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.store.AccountStore;
import lan.chaos.finance.common.store.SubjectStore;
import lan.chaos.finance.ledger.LedgerSamples;
import lan.chaos.finance.ledger.ReconcileLedger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 控制台演示：日终批处理（断点续跑）+ 审计脱敏（P4）。正确性由 {@code *Test} 断言。 */
@Component
@org.springframework.core.annotation.Order(6)
public class BatchAndAuditDemoRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("===== [batch/audit] 日终 + 审计演示（P4） =====");

        AccountStore accounts = new AccountStore();
        SubjectStore subjects = new SubjectStore();
        subjects.save(LedgerSamples.sampleSubject());
        accounts.save(LedgerSamples.sampleAccount("A", "alice", "1001"));

        CheckpointStore checkpoint = new CheckpointStore();
        List<String> handled = new ArrayList<>();
        DayEndJob job = new DayEndJob();
        // 第一轮：在 D3 处模拟宕机
        try {
            job.run(LocalDate.now(), new ReconcileLedger(accounts, subjects),
                    DayEndJob.sampleItems(), checkpoint, Money.of("0.00", Currency.CNY),
                    (WorkItem w) -> handled.add(w.getId()), "D3");
        } catch (IllegalStateException e) {
            System.out.println("第一轮: " + e.getMessage() + "，已处理 " + handled);
        }
        // 第二轮：断点续跑，不重不丢
        DayEndJob.DayEndReport report = job.run(LocalDate.now(),
                new ReconcileLedger(accounts, subjects), DayEndJob.sampleItems(), checkpoint,
                Money.of("0.00", Currency.CNY), (WorkItem w) -> handled.add(w.getId()), null);
        System.out.println("第二轮: " + report.getBatchResult() + "，总处理条目=" + handled.size() + "（应为 5，无重复）");

        AuditLogger audit = new AuditLogger();
        audit.record("teller01", "PAY", "BIZ-PAY-001", "6222021234567890");
        System.out.println("脱敏卡号: " + MaskUtils.maskCard("6222021234567890"));
        System.out.println("审计明细: " + audit.snapshot().get(0).getMaskedDetail() + "（不含完整卡号）");
        System.out.println("===== [batch/audit] 演示结束 =====");
    }
}
