package lan.chaos.microservice.user.service.impl;

import lan.chaos.microservice.common.core.exception.BizException;
import lan.chaos.microservice.common.core.result.ResultCode;
import lan.chaos.microservice.user.entity.Account;
import lan.chaos.microservice.user.mapper.AccountMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 账户扣减单元测试（离线，Mock 掉 Mapper）。
 *
 * <p>这是 Seata 全局事务里「用户侧分支」的核心业务规则：先查后扣、余额不足/账户不存在抛业务异常。
 * 异常经 Feign 传回订单侧后，会触发整个全局事务回滚——本测试验证「异常能被正确抛出」这一步。</p>
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountMapper accountMapper;
    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void deduct_ok_when_balance_enough() {
        Account account = new Account();
        account.setUserId(1L);
        account.setBalance(new BigDecimal("1000.00"));
        when(accountMapper.selectOne(any())).thenReturn(account);

        accountService.deduct(1L, new BigDecimal("200.00"));

        assertEquals(0, account.getBalance().compareTo(new BigDecimal("800.00")));
        verify(accountMapper).updateById(account);
    }

    @Test
    void deduct_throws_when_balance_not_enough() {
        Account account = new Account();
        account.setUserId(1L);
        account.setBalance(new BigDecimal("100.00"));
        when(accountMapper.selectOne(any())).thenReturn(account);

        BizException ex = assertThrows(BizException.class,
                () -> accountService.deduct(1L, new BigDecimal("500.00")));

        assertEquals(ResultCode.BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
        verify(accountMapper, never()).updateById(any());
    }

    @Test
    void deduct_throws_when_account_absent() {
        when(accountMapper.selectOne(any())).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> accountService.deduct(1L, new BigDecimal("1.00")));

        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }
}
