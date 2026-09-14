package lan.chaos.microservice.user.service;

import java.math.BigDecimal;

/**
 * 账户余额服务（Seata 全局事务的「用户侧分支资源」）。
 */
public interface AccountService {

    /**
     * 扣减指定用户的账户余额。
     *
     * @param userId 用户 id
     * @param amount 扣减金额（元）
     */
    void deduct(Long userId, BigDecimal amount);
}
