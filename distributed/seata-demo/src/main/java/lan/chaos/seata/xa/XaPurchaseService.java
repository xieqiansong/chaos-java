package lan.chaos.seata.xa;

import io.seata.spring.annotation.GlobalTransactional;
import lan.chaos.seata.at.AccountService;
import lan.chaos.seata.at.OrderService;
import lan.chaos.seata.at.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * XA 模式业务服务 — 演示 XA 场景下的代码形态。
 *
 * <h3>XA 与 AT 的本质区别</h3>
 * <table>
 *   <tr><th>维度</th><th>AT</th><th>XA</th></tr>
 *   <tr><td>一致性</td><td>最终一致（undo_log 补偿）</td><td>强一致（数据库 XA 协议）</td></tr>
 *   <tr><td>一阶段</td><td>记录镜像 + 全局锁，本地提交</td><td>各库 Prepare，全局 Commit / Rollback</td></tr>
 *   <tr><td>锁</td><td>Seata 全局锁</td><td>数据库原生行锁（提交前长期持有）</td></tr>
 *   <tr><td>适用场景</td><td>大部分业务</td><td>强一致、事务短、可接受持锁</td></tr>
 * </table>
 *
 * <p><b>关键认知：XA 模式的业务代码与 AT 完全一致</b>——同样使用 @GlobalTransactional，
 * 区别仅在底层数据源代理（DataSourceProxyXA 而非 DataSourceProxy）与数据库是否支持 XA 协议。</p>
 *
 * <p>启用 XA（见 README XA 章节）：</p>
 * <ol>
 *   <li>数据源改用 XA 代理：{@code new DataSourceProxyXA(rawDataSource)}（见 XaDataSourceConfig）</li>
 *   <li>关闭 starter 自动 AT 代理：{@code seata.enable-auto-data-source-proxy: false}</li>
 *   <li>数据库需支持 XA 协议（MySQL InnoDB / Oracle / H2 支持）</li>
 * </ol>
 *
 * @author chaos
 */
@Service
public class XaPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(XaPurchaseService.class);

    private final AccountService accountService;
    private final OrderService orderService;
    private final StorageService storageService;

    public XaPurchaseService(AccountService accountService,
                             OrderService orderService,
                             StorageService storageService) {
        this.accountService = accountService;
        this.orderService = orderService;
        this.storageService = storageService;
    }

    /**
     * XA 购买流程：扣款 → 下单 → 扣库存，由数据库 XA 协议保证强一致。
     */
    @GlobalTransactional(timeoutMills = 300000, rollbackFor = Exception.class)
    public String purchase(String userId, String productId, double amount, int count) {
        log.info("=== [XA] 购买开始: userId={}, productId={}, amount={}, count={} ===",
                userId, productId, amount, count);
        accountService.deduct(userId, amount);
        String orderNo = orderService.create(userId, productId, amount);
        storageService.deduct(productId, count);
        log.info("=== [XA] 购买完成: orderNo={} ===", orderNo);
        return orderNo;
    }
}
