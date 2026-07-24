package lan.chaos.microservice.order.controller;

import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.common.core.result.ResultCode;
import lan.chaos.microservice.order.model.Order;
import lan.chaos.microservice.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 订单 HTTP 入口。响应经 common-web 的 {@code ResponseAdvice} 统一包成 {@code R}。
 *
 * <p>演示：通过网关（8080）压测 {@code POST /orders} 看网关 Sentinel 限流；手动停掉 user 服务后，
 * 再下单观察返回 {@code DEGRADED} 而非 500（Feign 熔断降级生效）。</p>
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public R<Order> create(@RequestParam Long userId,
                           @RequestParam(required = false, defaultValue = "9.90") BigDecimal amount) {
        return R.ok(orderService.create(userId, amount));
    }

    /**
     * P3 分布式事务：创建订单（写 MySQL）+ 扣账户（写 PG），串成 Seata 全局事务。
     *
     * <p>正常：返回创建的订单；余额不足（user 返回 409）或 user 不可用时，全局事务回滚，
     * 响应 {@code 409 BALANCE_NOT_ENOUGH}（订单、账户两库都回到事务前状态）。</p>
     */
    @PostMapping("/tx")
    public R<Order> createWithTx(@RequestParam Long userId,
                                 @RequestParam(required = false, defaultValue = "9.90") BigDecimal amount) {
        return R.ok(orderService.createWithTx(userId, amount));
    }

    @GetMapping("/{id}")
    public R<Order> get(@PathVariable Long id) {
        Order order = orderService.get(id);
        if (order == null) {
            return R.fail(ResultCode.NOT_FOUND);
        }
        return R.ok(order);
    }
}
