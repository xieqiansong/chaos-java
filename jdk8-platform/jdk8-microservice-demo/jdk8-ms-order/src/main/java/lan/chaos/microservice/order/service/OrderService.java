package lan.chaos.microservice.order.service;

import lan.chaos.microservice.order.model.Order;

import java.math.BigDecimal;

public interface OrderService {

    /** 创建订单，并编排调用 user 服务填充用户名（user 降级时订单仍创建，仅标记 DEGRADED） */
    Order create(Long userId, BigDecimal amount);

    /** 按 id 查询订单 */
    Order get(Long id);
}
