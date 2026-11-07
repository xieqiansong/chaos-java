package lan.chaos.finance.common.store;

import lan.chaos.finance.common.model.ClearingBill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 清算单存储（以 bizNo 为幂等键）。
 *
 * <p>WHY：TCC 的三个阶段都可能因网络重试被重复调用，必须靠 bizNo 幂等 ——
 * 这就是 TCC 经典的「幂等 / 空回滚 / 悬挂」三问题里最基础的一环。
 */
public class ClearingBillStore {
    private final Map<String, ClearingBill> map = new ConcurrentHashMap<>();

    /** 幂等写入：已存在直接返回旧对象，保证三阶段重复调用不重复生效。 */
    public ClearingBill putIfAbsent(ClearingBill bill) {
        ClearingBill existing = map.putIfAbsent(bill.getBizNo(), bill);
        return existing == null ? bill : existing;
    }

    public ClearingBill get(String bizNo) {
        return map.get(bizNo);
    }
}
