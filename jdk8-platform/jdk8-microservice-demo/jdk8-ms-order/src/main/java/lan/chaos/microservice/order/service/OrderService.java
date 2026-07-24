package lan.chaos.microservice.order.service;

import lan.chaos.microservice.order.model.Order;

import java.math.BigDecimal;

public interface OrderService {

    /** 创建订单，并编排调用 user 服务填充用户名（user 降级时订单仍创建，仅标记 DEGRADED） */
    Order create(Long userId, BigDecimal amount);

    /**
     * P3 分布式事务：创建订单（写 MySQL）+ 调 user 扣减余额（写 PG），串成 Seata 全局事务。
     * 余额不足或 user 不可用时整个全局事务回滚（订单、账户两库一起撤销）。
     */
    Order createWithTx(Long userId, BigDecimal amount);

    /** 按 id 查询订单 */
    Order get(Long id);
}
