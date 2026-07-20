package lan.chaos.seata.tcc;

import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static lan.chaos.seata.common.constant.SeataConstants.*;

/**
 * 业务编排服务 — TCC 模式。
 *
 * <p>职责：用 @GlobalTransactional 协调三个 TCC 资源的 Try/Confirm/Cancel。</p>
 *
 * <h3>TCC 执行流程</h3>
 * <ol>
 *   <li>TM 开启全局事务，TC 返回 XID</li>
 *   <li>顺序调用 AccountTccAction.prepare → OrderTccAction.prepare → StorageTccAction.prepare</li>
 *   <li><b>所有 Try 成功</b> → TC 通知各 RM 执行 Confirm</li>
 *   <li><b>任一 Try 失败</b> → TC 通知已成功的 RM 执行 Cancel 回滚</li>
 * </ol>
 *
 * <h3>TCC 空回滚问题</h3>
 * <p>如果 Try 阶段因网络超时未执行但 TC 触发了 Cancel，此时 frozen=0，
 * 需要 Cancel 方法能正确处理这种"空回滚"——本实现通过 WHERE frozen >= ? 实现，不会报错。</p>
 *
 * @author chaos
 */
@Service
public class BusinessTccService {

    private static final Logger log = LoggerFactory.getLogger(BusinessTccService.class);

    private final AccountTccAction accountTccAction;
    private final OrderTccAction orderTccAction;
    private final StorageTccAction storageTccAction;

    public BusinessTccService(AccountTccAction accountTccAction,
                              OrderTccAction orderTccAction,
                              StorageTccAction storageTccAction) {
        this.accountTccAction = accountTccAction;
        this.orderTccAction = orderTccAction;
        this.storageTccAction = storageTccAction;
    }

    /**
     * 【场景 1】TCC 正常流程：Try → Confirm。
     */
    @GlobalTransactional(timeoutMills = 300000, rollbackFor = Exception.class)
    public boolean purchase(String userId, String productId, double amount, int count) {
        log.info("=== [TCC] 正常流程开始: userId={}, productId={}, amount={}, count={} ===",
                userId, productId, amount, count);
        accountTccAction.prepare(userId, amount);
        orderTccAction.prepare(userId, productId, amount);
        storageTccAction.prepare(productId, count);
        log.info("=== [TCC] Try 全部成功，等待 Confirm ===");
        return true;
    }

    /**
     * 【场景 2】TCC 回滚流程：余额不足 → Try 失败 → Cancel 已成功的 Try。
     */
    @GlobalTransactional(timeoutMills = 300000, rollbackFor = Exception.class)
    public boolean purchaseFail(String userId, String productId, double amount, int count) {
        log.warn("=== [TCC] 回滚流程开始: userId={}（预期 Try 失败）===", userId);
        try {
            accountTccAction.prepare(userId, amount);
            orderTccAction.prepare(userId, productId, amount);
            storageTccAction.prepare(productId, count);
            log.info("=== [TCC] 未预期成功 ===");
            return true;
        } catch (RuntimeException e) {
            log.warn("[TCC] Try 失败，触发 Cancel: {}", e.getMessage());
            throw e;
        }
    }
}
