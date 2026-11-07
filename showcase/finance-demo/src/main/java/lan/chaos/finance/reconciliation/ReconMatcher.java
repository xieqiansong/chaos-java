package lan.chaos.finance.reconciliation;

import lan.chaos.finance.common.constant.DiffType;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.ReconRecord;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对账勾对（P3，对应对话「对账 Reconciliation」）。
 *
 * <p>WHY：前面所有一致性机制（幂等/状态机/TCC）都可能漏，对账是<b>最后一道兜底</b>：
 * 定时比对"本方流水"与"渠道流水"，把差异分类后交差错处理。
 *
 * <p>差异分类（按业务流水号 bizNo 勾对）：
 * <ul>
 *   <li>{@link DiffType#NONE}：双方都有且金额一致；</li>
 *   <li>{@link DiffType#LONG} 长款：仅本方有（我方多记）；</li>
 *   <li>{@link DiffType#SHORT} 短款：仅渠道有（我方少记/漏记）；</li>
 *   <li>{@link DiffType#MISMATCH} 金额不符：双方都有但金额不等。</li>
 * </ul>
 */
public class ReconMatcher {

    public List<ReconRecord> match(Map<String, Money> local, Map<String, Money> channel) {
        List<ReconRecord> records = new ArrayList<>();
        Set<String> allBizNos = new LinkedHashSet<>();
        allBizNos.addAll(local.keySet());
        allBizNos.addAll(channel.keySet());

        for (String bizNo : allBizNos) {
            Money localAmount = local.get(bizNo);
            Money channelAmount = channel.get(bizNo);
            ReconRecord record = new ReconRecord();
            record.setBizNo(bizNo);
            record.setLocalAmount(localAmount);
            record.setChannelAmount(channelAmount);

            if (localAmount != null && channelAmount == null) {
                record.setDiffType(DiffType.LONG);
            } else if (localAmount == null && channelAmount != null) {
                record.setDiffType(DiffType.SHORT);
            } else if (localAmount.compareTo(channelAmount) != 0) {
                record.setDiffType(DiffType.MISMATCH);
            } else {
                record.setDiffType(DiffType.NONE);
            }
            records.add(record);
        }
        return records;
    }

    /** 统计各差异类型的笔数，便于断言与观察。 */
    public Map<DiffType, Long> countByType(List<ReconRecord> records) {
        Map<DiffType, Long> counts = new java.util.EnumMap<>(DiffType.class);
        for (DiffType t : DiffType.values()) {
            counts.put(t, 0L);
        }
        for (ReconRecord r : records) {
            counts.merge(r.getDiffType(), 1L, Long::sum);
        }
        return counts;
    }
}
