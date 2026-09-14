package lan.chaos.seata.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SAGA 业务编排 — 正向执行 + 逆向补偿链（演示 SAGA 机制）。
 *
 * <h3>SAGA 与 AT/TCC 的本质区别</h3>
 * <table>
 *   <tr><th>维度</th><th>AT</th><th>TCC</th><th>SAGA</th></tr>
 *   <tr><td>一致性</td><td>强一致（全局锁 + undo_log）</td><td>最终一致（手动二阶段）</td><td>最终一致（补偿链）</td></tr>
 *   <tr><td>一阶段</td><td>记录镜像、持全局锁</td><td>预留资源（frozen）</td><td>直接提交本地事务</td></tr>
 *   <tr><td>回滚</td><td>undo_log 自动反向补偿</td><td>Cancel 手动解冻</td><td>逆序调用补偿方法</td></tr>
 *   <tr><td>适用场景</td><td>短事务 CRUD</td><td>性能敏感 / 跨系统对接</td><td>长事务、无锁、容忍中间状态</td></tr>
 * </table>
 *
 * <p><b>正向流程</b>：扣款 → 建单 → 扣库存，每步独立本地事务，执行完即提交。
 * <b>失败补偿</b>：某步失败后，对<b>已成功执行</b>的步骤按<b>严格逆序</b>调用补偿方法。</p>
 *
 * <p>为什么必须逆序补偿？正向步骤间存在依赖（先扣款才能下单），
 * 撤销时必须先撤销最后成功的一步，才能安全回滚前面的步骤。</p>
 *
 * <p>为什么只补偿"已成功"的步骤？若扣库存失败，库存本来就未扣减，
 * 再去"补偿扣库存"反而会把数据改错，因此用 {@code done} 记录成功轨迹。</p>
 *
 * <p>生产化：Seata 官方提供 SAGA 状态机引擎（seata-saga-statelang / engine + JSON 状态机），
 * 支持分支、并行、超时与持久化；本 Demo 用手写编排聚焦补偿链机制本身。</p>
 *
 * @author chaos
 */
@Service
public class SagaBusinessService {

    private static final Logger log = LoggerFactory.getLogger(SagaBusinessService.class);

    /** 正向步骤标识（用于失败时逆序补偿）。 */
    private enum Step { ACCOUNT, ORDER, STORAGE }

    private final SagaAccountService accountService;
    private final SagaOrderService orderService;
    private final SagaStorageService storageService;

    public SagaBusinessService(SagaAccountService accountService,
                               SagaOrderService orderService,
                               SagaStorageService storageService) {
        this.accountService = accountService;
        this.orderService = orderService;
        this.storageService = storageService;
    }

    /**
     * SAGA 购买流程：扣款 → 建单 → 扣库存。
     *
     * @return 订单号
     * @throws RuntimeException 任一步骤失败时，已成功步骤被逆序补偿后重新抛出
     */
    public String purchase(String userId, String productId, double amount, int count) {
        log.info("=== [SAGA] 正向流程开始: userId={}, productId={}, amount={}, count={} ===",
                userId, productId, amount, count);
        String orderNo = UUID.randomUUID().toString().replace("-", "");
        List<Step> done = new ArrayList<>();
        try {
            accountService.deduct(userId, amount);            // 正向 1：扣款
            done.add(Step.ACCOUNT);
            orderService.create(userId, productId, amount, orderNo); // 正向 2：建单
            done.add(Step.ORDER);
            storageService.deduct(productId, count);          // 正向 3：扣库存
            done.add(Step.STORAGE);
            log.info("=== [SAGA] 正向全部成功: orderNo={} ===", orderNo);
            return orderNo;
        } catch (RuntimeException e) {
            log.warn("[SAGA] 正向失败: {}，对已成功步骤逆序补偿", e.getMessage());
            compensate(done, userId, productId, amount, count, orderNo);
            throw e;
        }
    }

    /**
     * 逆向补偿链：对已成功步骤按与正向相反的顺序执行补偿。
     */
    private void compensate(List<Step> done, String userId, String productId,
                            double amount, int count, String orderNo) {
        for (int i = done.size() - 1; i >= 0; i--) {
            switch (done.get(i)) {
                case STORAGE: storageService.compensateDeduct(productId, count); break;
                case ORDER:   orderService.compensateCreate(orderNo);            break;
                case ACCOUNT: accountService.compensateDeduct(userId, amount);   break;
                default:      break;
            }
        }
    }
}
