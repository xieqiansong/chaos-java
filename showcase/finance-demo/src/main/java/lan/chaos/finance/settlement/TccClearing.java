package lan.chaos.finance.settlement;

import lan.chaos.finance.common.constant.ClearingStatus;
import lan.chaos.finance.common.exception.ClearingException;
import lan.chaos.finance.common.model.ClearingBill;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.Position;
import lan.chaos.finance.common.store.ClearingBillStore;
import lan.chaos.finance.common.store.PositionStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 手写最小 TCC 清算（P3，对应对话「最终一致性 / TCC」）。
 *
 * <p>WHY：交易→账务→清算跨服务，一个本地事务锁不住全部。TCC 把一次资金划拨拆成：
 * <ul>
 *   <li><b>Try</b>：把付款方资金从「可用」挪到「冻结」（占住，不真扣）；</li>
 *   <li><b>Confirm</b>：真正扣减可用、增加收款方，冻结释放；</li>
 *   <li><b>Cancel</b>：解冻归还，资金原路返回。</li>
 * </ul>
 * 任一阶段失败都能回到守恒状态，避免"扣了钱没到账"或"钱凭空消失"。
 *
 * <p>TCC 三大坑，本实现逐一处理：
 * <ul>
 *   <li><b>幂等</b>：三阶段均以 bizNo 幂等（{@link ClearingBillStore#putIfAbsent}）；
 *       Confirm/Cancel 重复调用直接返回，不重复动账。</li>
 *   <li><b>空回滚</b>：Cancel 时若 Try 从未到达，记录一条 CANCELED 的"空回滚"标记，
 *       不抛异常也不动账，防止后续迟到的 Try 又把钱冻上。</li>
 *   <li><b>悬挂</b>：Try 时若发现该 bizNo 已被空回滚标记，直接拒绝，
 *       保证 Cancel 永远先于迟到的 Try 生效。</li>
 * </ul>
 *
 * <p>防御性思维②：中途失败留什么中间状态？→ TRY 态 + 冻结资金，由对账/定时任务
 * 扫描超时 TRY 单触发 Cancel，不悬挂。
 *
 * <p>防御性思维①（并发）：三个方法共享 {@code synchronized (billStore)} 同一个临界区，
 * 把「幂等检查 + 头寸变动 + 单据写入」做成原子操作。若把幂等检查放在锁外，
 * 两个线程会同时通过检查、各自冻结一次，而 {@code putIfAbsent} 只让一张单生效 ——
 * 结果「冻结两次、只释放一次」，剩余资金永久挂账（资损）。详见并发回归测试。
 */
public class TccClearing {

    private final PositionStore positionStore;
    private final ClearingBillStore billStore;
    /** 空回滚标记：bizNo → true，用于拦截迟到的 Try（防悬挂）。 */
    private final Map<String, Boolean> emptyRollbackMarks = new ConcurrentHashMap<>();

    public TccClearing(PositionStore positionStore, ClearingBillStore billStore) {
        this.positionStore = positionStore;
        this.billStore = billStore;
    }

    /** 一阶段：冻结付款方头寸。 */
    public ClearingBill tryFreeze(String bizNo, String from, String to, Money amount) {
        synchronized (billStore) {
            // 防悬挂：Cancel 已先到达（空回滚），迟到的 Try 必须拒绝
            if (emptyRollbackMarks.containsKey(bizNo)) {
                throw new ClearingException("该清算单已空回滚，拒绝迟到的 Try（防悬挂）: " + bizNo);
            }
            ClearingBill existing = billStore.get(bizNo);
            if (existing != null) {
                return existing; // Try 幂等（锁内二次检查，杜绝重复冻结）
            }
            ClearingBill bill = new ClearingBill();
            bill.setBizNo(bizNo);
            bill.setFromInstitution(from);
            bill.setToInstitution(to);
            bill.setAmount(amount);
            bill.setStatus(ClearingStatus.TRY);

            Position payer = positionStore.get(from);
            if (payer.getAvailable().compareTo(amount) < 0) {
                throw new ClearingException("可用头寸不足，冻结失败: " + from);
            }
            payer.setAvailable(payer.getAvailable().subtract(amount));
            payer.setFrozen(payer.getFrozen().add(amount));
            return billStore.putIfAbsent(bill);
        }
    }

    /** 二阶段提交：实际划拨。 */
    public ClearingBill confirm(String bizNo) {
        synchronized (billStore) {
            ClearingBill bill = requireBill(bizNo);
            if (bill.getStatus() == ClearingStatus.CONFIRMED) {
                return bill; // Confirm 幂等
            }
            if (bill.getStatus() != ClearingStatus.TRY) {
                throw new ClearingException("仅 TRY 态可 Confirm，当前=" + bill.getStatus());
            }
            Position payer = positionStore.get(bill.getFromInstitution());
            Position payee = positionStore.get(bill.getToInstitution());
            // 冻结释放（Try 已从 available 扣走，这里只需清掉 frozen）
            payer.setFrozen(payer.getFrozen().subtract(bill.getAmount()));
            // 收款方入账
            payee.setAvailable(payee.getAvailable().add(bill.getAmount()));
            bill.setStatus(ClearingStatus.CONFIRMED);
            return bill;
        }
    }

    /** 二阶段回滚：解冻归还；Try 未到达时记录空回滚标记。 */
    public ClearingBill cancel(String bizNo) {
        synchronized (billStore) {
            ClearingBill bill = billStore.get(bizNo);
            if (bill == null) {
                // 空回滚：Try 从未到达，只打标记，不动账
                emptyRollbackMarks.put(bizNo, Boolean.TRUE);
                return null;
            }
            if (bill.getStatus() == ClearingStatus.CANCELED) {
                return bill; // Cancel 幂等
            }
            if (bill.getStatus() != ClearingStatus.TRY) {
                throw new ClearingException("仅 TRY 态可 Cancel，当前=" + bill.getStatus());
            }
            Position payer = positionStore.get(bill.getFromInstitution());
            payer.setFrozen(payer.getFrozen().subtract(bill.getAmount()));
            payer.setAvailable(payer.getAvailable().add(bill.getAmount()));
            bill.setStatus(ClearingStatus.CANCELED);
            return bill;
        }
    }

    private ClearingBill requireBill(String bizNo) {
        ClearingBill bill = billStore.get(bizNo);
        if (bill == null) {
            throw new ClearingException("清算单不存在: " + bizNo);
        }
        return bill;
    }
}
