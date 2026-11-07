package lan.chaos.finance.common.store;

import lan.chaos.finance.common.model.TradeOrder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 交易订单存储 + 幂等键（以 bizNo 唯一）。
 *
 * <p>WHY：金融幂等的第一道兜底是「数据库唯一键约束」—— 同一业务流水号只能落一条。
 * 这里用 {@link ConcurrentHashMap#putIfAbsent} 模拟该约束：所有调用者拿到的是
 * <b>同一个</b>订单对象，从而能以它为锁把"扣款"串行化（见 {@code PaymentService}）。
 */
public class PaymentOrderStore {
    private final Map<String, TradeOrder> map = new ConcurrentHashMap<>();

    /**
     * 若 bizNo 不存在则创建并返回新对象；否则返回已存在的对象（幂等命中）。
     */
    public TradeOrder getOrCreate(String bizNo, Supplier<TradeOrder> factory) {
        TradeOrder existing = map.get(bizNo);
        if (existing != null) {
            return existing;
        }
        TradeOrder created = factory.get();
        TradeOrder winner = map.putIfAbsent(bizNo, created);
        return winner == null ? created : winner;
    }

    public TradeOrder findByBizNo(String bizNo) {
        return map.get(bizNo);
    }

    public int size() {
        return map.size();
    }
}
