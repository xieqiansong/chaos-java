package lan.chaos.microservice.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lan.chaos.microservice.common.core.exception.BizException;
import lan.chaos.microservice.common.core.result.ResultCode;
import lan.chaos.microservice.user.entity.Account;
import lan.chaos.microservice.user.mapper.AccountMapper;
import lan.chaos.microservice.user.service.AccountService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 账户余额服务实现（Seata 全局事务的「用户侧分支资源」，写在主库 PG）。
 *
 * <p>WHY（分布式事务里的角色）：</p>
 * <ul>
 *   <li>本方法由订单服务的 {@code @GlobalTransactional} 通过 Feign 调用，xid 已由
 *       {@code spring-cloud-starter-alibaba-seata} 的拦截器自动透传，因此这里天然处于全局事务上下文中；</li>
 *   <li>ms-user 开启了 {@code spring.datasource.dynamic.seata=true}，本次对 t_account 的更新会被 Seata
 *       自动登记为「分支事务」并写入 undo_log，<b>本方法无需自己加 @GlobalTransactional</b>；</li>
 *   <li>一旦订单侧抛异常触发全局回滚，Seata TC 会通知本分支按 undo_log 反向补偿，账户余额被还原——实现跨库回滚。</li>
 * </ul>
 *
 * <p>生产坑点：</p>
 * <ul>
 *   <li>余额校验必须在本地事务内做（先查后扣），否则高并发下会超扣；AT 模式靠全局锁兜底，但仍建议业务层先判余额；</li>
 *   <li>余额不足时抛 {@link BizException#BALANCE_NOT_ENOUGH}，异常经 Feign 传到订单侧触发回滚；</li>
 *   <li>扣减一定要用「先查后更新」而非「UPDATE ... SET balance=balance-?」，避免丢失更新。</li>
 * </ul>
 */
@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountMapper accountMapper;

    @Override
    public void deduct(Long userId, BigDecimal amount) {
        Account account = accountMapper.selectOne(
                new LambdaQueryWrapper<Account>().eq(Account::getUserId, userId));
        if (account == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "账户不存在: userId=" + userId);
        }
        if (account.getBalance().compareTo(amount) < 0) {
            // 余额不足：抛业务异常，经 Feign 传到订单侧 -> 触发全局事务回滚
            throw new BizException(ResultCode.BALANCE_NOT_ENOUGH);
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountMapper.updateById(account);
    }
}
