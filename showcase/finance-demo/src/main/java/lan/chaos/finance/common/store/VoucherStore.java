package lan.chaos.finance.common.store;

import lan.chaos.finance.common.model.Voucher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 凭证内存存储（演示用，append-only 语义）。生产环境替换为 JPA/H2/真实数据库，接口不变。
 */
public class VoucherStore {
    private final Map<String, Voucher> map = new ConcurrentHashMap<>();

    public Voucher save(Voucher voucher) {
        map.put(voucher.getId(), voucher);
        return voucher;
    }

    public Voucher get(String id) {
        Voucher voucher = map.get(id);
        if (voucher == null) {
            throw new IllegalArgumentException("凭证不存在: " + id);
        }
        return voucher;
    }
}
