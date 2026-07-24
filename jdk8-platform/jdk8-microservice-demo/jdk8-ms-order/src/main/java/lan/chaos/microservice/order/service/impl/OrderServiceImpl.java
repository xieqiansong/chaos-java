package lan.chaos.microservice.order.service.impl;

import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.order.client.UserClient;
import lan.chaos.microservice.order.model.Order;
import lan.chaos.microservice.order.model.UserDTO;
import lan.chaos.microservice.order.service.OrderService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单服务实现。
 *
 * <p>WHY（编排 + 降级）：创建订单不是单纯写库，还要“拉一下 user 服务”补全用户名。
 * 若 user 不可用，Feign 走 {@link UserClientFallbackFactory} 返回降级 R，这里优雅地把订单标记为
 * {@code DEGRADED} 而非让整条创建链路 500——这就是熔断/降级要解决的“下游挂了上游还能活”。</p>
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final UserClient userClient;
    private final Map<Long, Order> store = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    public OrderServiceImpl(UserClient userClient) {
        this.userClient = userClient;
    }

    @Override
    public Order create(Long userId, BigDecimal amount) {
        Order order = Order.sample(userId, amount);
        order.setId(idGen.getAndIncrement());

        // 编排调用 user 服务；user 不可用时 userClient 直接返回降级 R（不抛异常）
        R<UserDTO> resp = userClient.getUser(userId);
        if (resp != null && resp.getCode() == 0 && resp.getData() != null) {
            order.setUsername(resp.getData().getUsername());
            order.setStatus("CREATED");
        } else {
            order.setStatus("DEGRADED");
        }
        store.put(order.getId(), order);
        return order;
    }

    @Override
    public Order get(Long id) {
        return store.get(id);
    }
}
