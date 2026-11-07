package lan.chaos.finance.reconciliation;

import lan.chaos.finance.common.model.ReconRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * 差错处理钩子（P3 演示）。
 *
 * <p>WHY：对账发现差异后进入差错处理流程。真实系统会按差异类型做
 * 自动冲正 / 补记账 / 挂账待人工复核；这里只记录动作，不接真实账务，
 * 保持「最小可用」且可断言（进阶方向见 README）。
 */
public class DiffHandler {

    private final List<String> actions = new ArrayList<>();

    public void handle(List<ReconRecord> records) {
        for (ReconRecord r : records) {
            switch (r.getDiffType()) {
                case NONE:
                    break;
                case LONG:
                    actions.add("长款挂账待核: " + r.getBizNo() + " 本方 " + r.getLocalAmount());
                    break;
                case SHORT:
                    actions.add("短款补记账: " + r.getBizNo() + " 渠道 " + r.getChannelAmount());
                    break;
                case MISMATCH:
                    actions.add("金额不符待核: " + r.getBizNo()
                            + " 本方 " + r.getLocalAmount() + " 渠道 " + r.getChannelAmount());
                    break;
                default:
                    throw new IllegalStateException("未知差异类型: " + r.getDiffType());
            }
        }
    }

    public List<String> getActions() {
        return actions;
    }
}
