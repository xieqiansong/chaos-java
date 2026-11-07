package lan.chaos.finance.common.store;

import lan.chaos.finance.common.model.Account;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分户账内存存储（演示用）。生产环境替换为 JPA/H2/真实数据库，接口不变。
 */
public class AccountStore {
    private final Map<String, Account> map = new ConcurrentHashMap<>();

    public Account save(Account account) {
        map.put(account.getId(), account);
        return account;
    }

    public Account get(String id) {
        Account account = map.get(id);
        if (account == null) {
            throw new IllegalArgumentException("账户不存在: " + id);
        }
        return account;
    }

    public Collection<Account> all() {
        return map.values();
    }
}
