package lan.chaos.microservice.order.service.impl;

import io.seata.spring.annotation.GlobalTransactional;
import lan.chaos.microservice.common.core.exception.BizException;
import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.common.core.result.ResultCode;
import lan.chaos.microservice.order.client.UserClient;
import lan.chaos.microservice.order.mapper.OrderMapper;
import lan.chaos.microservice.order.model.Order;
import lan.chaos.microservice.order.model.UserDTO;
import lan.chaos.microservice.order.service.OrderService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单服务实现。
 *
 * <p>WHY（编排 + 降级，P2）：{@link #create} 创建订单不是单纯写库，还要“拉一下 user 服务”补全用户名。
 * 若 user 不可用，Feign 走 {@link lan.chaos.microservice.order.client.UserClientFallbackFactory} 返回降级 R，
 * 这里优雅地把订单标记为 {@code DEGRADED} 而非让整条创建链路 500。</p>
 *
 * <p>WHY（分布式事务，P3）：{@link #createWithTx} 把“写订单（MySQL）”和“扣账户（PG，经 Feign 调 user）”
 * 串成一个 {@link GlobalTransactional}。任一分支失败，Seata 会让两个库按各自的 undo_log 一起回滚——
 * 这就是跨库跨服务的数据一致性，业务代码几乎无侵入（只多一个注解）。</p>
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final UserClient userClient;
    private final OrderMapper orderMapper;

    public OrderServiceImpl(UserClient userClient, OrderMapper orderMapper) {
        this.userClient = userClient;
        this.orderMapper = orderMapper;
    }

    @Override
    public Order create(Long userId, BigDecimal amount) {
        Order order = Order.sample(userId, amount);

        // P2 编排：拉 user 补全用户名；用户不可用则降级，订单仍创建（标记 DEGRADED），不抛异常
        R<UserDTO> resp = userClient.getUser(userId);
        if (resp != null && resp.getCode() == 0 && resp.getData() != null) {
            order.setUsername(resp.getData().getUsername());
            order.setStatus("CREATED");
        } else {
            order.setStatus("DEGRADED");
        }
        orderMapper.insert(order);
        return order;
    }

    @Override
    @GlobalTransactional(name = "create-order-tx", rollbackFor = Exception.class)
    public Order createWithTx(Long userId, BigDecimal amount) {
        // 1) 订单分支：写 MySQL t_order（本地分支，Seata 自动登记）
        Order order = Order.sample(userId, amount);
        order.setStatus("CREATED");
        orderMapper.insert(order);

        // 2) 账户分支：经 Feign 调 user 扣减余额。xid 由 Seata 的 Feign 拦截器自动透传，
        //    user 侧 dynamic.seata=true 会自动把这次 DB 写登记成同一全局事务的分支。
        //    余额不足 / user 降级都会让这里拿到非 0 的 R —— 必须抛异常才能触发全局回滚。
        R<Void> deduct = userClient.deductAccount(userId, amount);
        if (deduct == null || deduct.getCode() != 0) {
            // 抛异常 -> Seata 通知两分支按 undo_log 回滚：订单撤销 + 账户还原，保证两库一致
            throw new BizException(ResultCode.BALANCE_NOT_ENOUGH);
        }
        return order;
    }

    @Override
    public Order get(Long id) {
        return orderMapper.selectById(id);
    }
}
