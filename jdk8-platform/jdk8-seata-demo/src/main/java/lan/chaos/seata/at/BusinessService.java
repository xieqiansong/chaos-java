package lan.chaos.seata.at;

import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static lan.chaos.seata.common.constant.SeataConstants.*;

/**
 * 业务编排服务 — AT 模式。
 *
 * <p>职责：用 @GlobalTransactional 将多个本地事务编织为全局事务。</p>
 *
 * <h3>核心注解 @GlobalTransactional</h3>
 * <ul>
 *   <li><b>timeoutMills</b>：全局事务超时，默认 60s，超时自动回滚</li>
 *   <li><b>rollbackFor</b>：触发回滚的异常类型</li>
 *   <li>标注后，该方法内所有 @Transactional 操作受全局事务管理</li>
 * </ul>
 *
 * <h3>执行流程（AT 模式）</h3>
 * <ol>
 *   <li>TM（BusinessService）向 TC 申请开启全局事务，TC 返回 XID</li>
 *   <li>调用 AccountService → RM 注册分支事务、执行扣款、记录 undo_log</li>
 *   <li>调用 OrderService → RM 注册分支事务、创建订单、记录 undo_log</li>
 *   <li>调用 StorageService → RM 注册分支事务、扣库存、记录 undo_log</li>
 *   <li>全部成功 → TC 通知所有 RM 提交（异步清理 undo_log）</li>
 *   <li>任一失败 → TC 通知所有 RM 根据 undo_log 反向补偿回滚</li>
 * </ol>
 *
 * <p>提供了两个入口：
 * <ul>
 *   <li>{@link #purchase(String, String, double, int)} 正常购买</li>
 *   <li>{@link #purchaseFail(String, String, double, int)} 模拟库存不足触发回滚</li>
 * </ul>
 * </p>
 *
 * @author chaos
 */
@Service
public class BusinessService {

    private static final Logger log = LoggerFactory.getLogger(BusinessService.class);

    private final AccountService accountService;
    private final OrderService orderService;
    private final StorageService storageService;

    public BusinessService(AccountService accountService,
                           OrderService orderService,
                           StorageService storageService) {
        this.accountService = accountService;
        this.orderService = orderService;
        this.storageService = storageService;
    }

    /**
     * 【场景 1】正常购买：扣款 → 下单 → 扣库存，全部成功。
     *
     * @return 订单编号
     */
    @GlobalTransactional(timeoutMills = 300000, rollbackFor = Exception.class)
    public String purchase(String userId, String productId, double amount, int count) {
        log.info("=== [AT] 正常购买开始: userId={}, productId={}, amount={}, count={} ===",
                userId, productId, amount, count);
        accountService.deduct(userId, amount);
        String orderNo = orderService.create(userId, productId, amount);
        storageService.deduct(productId, count);
        log.info("=== [AT] 正常购买完成: orderNo={} ===", orderNo);
        return orderNo;
    }

    /**
     * 【场景 2】模拟失败：传入不存在的 userId，扣款时抛异常，
     * 全局事务回滚已验证的订单和库存操作。
     *
     * @return 订单编号（失败时为 null）
     */
    @GlobalTransactional(timeoutMills = 300000, rollbackFor = Exception.class)
    public String purchaseFail(String userId, String productId, double amount, int count) {
        log.warn("=== [AT] 失败回滚场景开始: userId={}（预期回滚）===", userId);
        try {
            accountService.deduct(userId, amount);
            String orderNo = orderService.create(userId, productId, amount);
            storageService.deduct(productId, count);
            log.info("=== [AT] 未预期成功 ===");
            return orderNo;
        } catch (RuntimeException e) {
            log.warn("[AT] 业务异常，触发全局回滚: {}", e.getMessage());
            throw e; // 必须继续向外抛，GlobalTransactional 才能感知
        }
    }
}
