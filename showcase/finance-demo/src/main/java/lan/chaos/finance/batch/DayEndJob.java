package lan.chaos.finance.batch;

import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.ledger.ReconcileLedger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 日终批处理（日切）作业（P4）。
 *
 * <p>WHY：银行白天跑联机交易，晚上在特定窗口做「日终」：总分核对、清算文件生成、
 * 报表产出等。这里把两件最核心的事串起来：
 * <ol>
 *   <li><b>总分核对</b>：调用 {@link ReconcileLedger} 校验分户账与总账勾稽；</li>
 *   <li><b>批量任务</b>：用 {@link ShardedRunner} 分片处理当日流水，支持断点续跑。</li>
 * </ol>
 */
public class DayEndJob {

    /** 日终报告（可观察输出）。 */
    public static class DayEndReport {
        private final LocalDate bizDate;
        private final boolean ledgerConsistent;
        private final ShardedRunner.Result batchResult;
        private final Money totalAmount;

        DayEndReport(LocalDate bizDate, boolean ledgerConsistent,
                     ShardedRunner.Result batchResult, Money totalAmount) {
            this.bizDate = bizDate;
            this.ledgerConsistent = ledgerConsistent;
            this.batchResult = batchResult;
            this.totalAmount = totalAmount;
        }

        public LocalDate getBizDate() {
            return bizDate;
        }

        public boolean isLedgerConsistent() {
            return ledgerConsistent;
        }

        public ShardedRunner.Result getBatchResult() {
            return batchResult;
        }

        public Money getTotalAmount() {
            return totalAmount;
        }
    }

    public DayEndReport run(LocalDate bizDate, ReconcileLedger reconcile,
                            List<WorkItem> items, CheckpointStore checkpoint,
                            Money zero, Consumer<WorkItem> handler, String failOnId) {
        // 1) 总分核对：不一致直接抛异常，日终不允许带病结束
        reconcile.assertConsistent();
        // 2) 分片批处理
        ShardedRunner.Result batch = new ShardedRunner().run(items, checkpoint, handler, failOnId);
        // 3) 汇总（演示报表口径）
        Money total = zero;
        for (WorkItem item : items) {
            total = total.add(Money.of(item.getPayload(), zero.getCurrency()));
        }
        return new DayEndReport(bizDate, true, batch, total);
    }

    /** 样例当日流水工作项（3 个分片）。 */
    public static List<WorkItem> sampleItems() {
        List<WorkItem> items = new ArrayList<>();
        items.add(item("D1", 0, "100.00"));
        items.add(item("D2", 0, "200.00"));
        items.add(item("D3", 1, "300.00"));
        items.add(item("D4", 1, "400.00"));
        items.add(item("D5", 2, "500.00"));
        return items;
    }

    private static WorkItem item(String id, int shard, String payload) {
        WorkItem w = new WorkItem();
        w.setId(id);
        w.setShard(shard);
        w.setPayload(payload);
        return w;
    }
}
