package lan.chaos.microservice.order.service.impl;

import lan.chaos.microservice.common.core.exception.BizException;
import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.common.core.result.ResultCode;
import lan.chaos.microservice.order.client.UserClient;
import lan.chaos.microservice.order.mapper.OrderMapper;
import lan.chaos.microservice.order.model.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单全局事务（Seata @GlobalTransactional）的业务编排单测（离线，Mock 掉 Feign 与 Mapper）。
 *
 * <p>纯单测下不会连接 Seata TC，@GlobalTransactional 仅作为标记存在。本测试验证的是「触发回滚的代码路径」：
 * 扣减成功则订单入库、全局提交；扣减返回非 0（余额不足/降级）则必须抛异常，该异常在真实环境驱动 Seata
 * 让订单分支按 undo_log 回滚，从而与账户分支保持一致。</p>
 */
@ExtendWith(MockitoExtension.class)
class OrderSeataRollbackTest {

    @Mock
    private UserClient userClient;
    @Mock
    private OrderMapper orderMapper;
    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createWithTx_success_when_deduct_ok() {
        when(userClient.deductAccount(1L, new BigDecimal("9.90"))).thenReturn(R.ok());

        Order order = orderService.createWithTx(1L, new BigDecimal("9.90"));

        assertNotNull(order);
        assertEquals("CREATED", order.getStatus());
        verify(orderMapper).insert(any(Order.class));
        verify(userClient).deductAccount(1L, new BigDecimal("9.90"));
    }

    @Test
    void createWithTx_throws_when_deduct_fails() {
        when(userClient.deductAccount(1L, new BigDecimal("99999")))
                .thenReturn(R.fail(ResultCode.BALANCE_NOT_ENOUGH));

        BizException ex = assertThrows(BizException.class,
                () -> orderService.createWithTx(1L, new BigDecimal("99999")));

        assertEquals(ResultCode.BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
        // 订单分支已开启（insert 已执行），异常将驱动 Seata 回滚该分支
        verify(orderMapper).insert(any(Order.class));
    }
}
