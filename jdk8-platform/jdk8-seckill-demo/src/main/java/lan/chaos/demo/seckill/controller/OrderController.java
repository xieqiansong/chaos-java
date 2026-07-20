package lan.chaos.demo.seckill.controller;

import lan.chaos.demo.seckill.dto.OrderResponse;
import lan.chaos.demo.seckill.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 订单控制器
 * <p>
 * GET /api/orders/{token} — 查询订单状态
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 根据令牌查询订单
     */
    @GetMapping("/{token}")
    public ResponseEntity<?> getOrder(@PathVariable String token) {
        Optional<OrderResponse> orderOpt = orderService.getOrderByToken(token);
        if (orderOpt.isPresent()) {
            return ResponseEntity.ok(orderOpt.get());
        }
        return ResponseEntity.status(404).body("订单不存在");
    }
}
