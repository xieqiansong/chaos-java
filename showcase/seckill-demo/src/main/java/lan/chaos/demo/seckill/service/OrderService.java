package lan.chaos.demo.seckill.service;

import lan.chaos.demo.seckill.dto.OrderResponse;
import lan.chaos.demo.seckill.entity.Product;
import lan.chaos.demo.seckill.entity.SeckillOrder;
import lan.chaos.demo.seckill.repository.SeckillOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务
 * <p>
 * 负责订单的异步落库、去重、状态管理。
 * 使用 Redis Set 记录已处理令牌防止重复提交。
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final String ORDER_TOKEN_SET_PREFIX = "seckill:order_token:";
    private static final String USER_BOUGHT_PREFIX = "seckill:user_bought:";

    private final SeckillOrderRepository orderRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderService(SeckillOrderRepository orderRepository,
                        StringRedisTemplate stringRedisTemplate,
                        KafkaTemplate<String, String> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    // ==================== 令牌管理 ====================

    /**
     * 检查令牌是否已被处理（去重）
     */
    public boolean isTokenProcessed(Long productId, String token) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(ORDER_TOKEN_SET_PREFIX + productId, token));
    }

    /**
     * 记录已处理令牌
     */
    public void markTokenProcessed(Long productId, String token) {
        stringRedisTemplate.opsForSet().add(ORDER_TOKEN_SET_PREFIX + productId, token);
        // 设置TTL防止内存泄漏（秒杀活动最长24小时）
        stringRedisTemplate.expire(ORDER_TOKEN_SET_PREFIX + productId, 24, TimeUnit.HOURS);
    }

    // ==================== 限购检查 ====================

    /**
     * 检查用户是否已购买（每人限购1件）
     */
    public boolean hasUserBought(Long productId, String userId) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().get(USER_BOUGHT_PREFIX + productId + ":" + userId) != null);
    }

    /**
     * 记录用户已购买
     */
    public void markUserBought(Long productId, String userId) {
        stringRedisTemplate.opsForValue().set(
                USER_BOUGHT_PREFIX + productId + ":" + userId, "1", 24, TimeUnit.HOURS);
    }

    // ==================== 异步下单 ====================

    /**
     * 异步发送订单消息到 Kafka
     */
    @Async
    public void asyncCreateOrder(Long orderId, Long productId, String userId,
                                  String token, int bucketIndex, BigDecimal amount) {
        // 构建消息体 productId,userId,token,bucketIndex,amount
        String message = String.format("%d,%s,%s,%d,%s", productId, userId, token, bucketIndex, amount);
        try {
            org.springframework.util.concurrent.ListenableFuture<org.springframework.kafka.support.SendResult<String, String>> future =
                    kafkaTemplate.send("seckill-order", userId, message);
            future.addCallback(
                    result -> {
                        if (result != null) {
                            log.debug("订单消息已发送: token={}, offset={}", token,
                                    result.getRecordMetadata().offset());
                        }
                    },
                    ex -> log.error("发送订单消息失败: token={}", token, ex)
            );
        } catch (Exception e) {
            log.error("发送订单消息异常: token={}", token, e);
        }
    }

    /**
     * 消费订单消息 — 异步落库
     */
    @KafkaListener(topics = "seckill-order", groupId = "seckill-order-group",
            concurrency = "4")
    @Transactional
    public void processOrder(String message) {
        try {
            String[] parts = message.split(",");
            Long productId = Long.parseLong(parts[0]);
            String userId = parts[1];
            String token = parts[2];
            int bucketIndex = Integer.parseInt(parts[3]);
            BigDecimal amount = new BigDecimal(parts[4]);

            // 去重检查
            if (isTokenProcessed(productId, token)) {
                log.warn("重复订单消息，已忽略: token={}", token);
                return;
            }

            // 创建订单
            SeckillOrder order = new SeckillOrder();
            order.setId(generateOrderId(token));
            order.setProductId(productId);
            order.setUserId(userId);
            order.setToken(token);
            order.setBucketIndex(bucketIndex);
            order.setStatus("CONFIRMED");
            order.setAmount(amount);

            orderRepository.save(order);
            markTokenProcessed(productId, token);
            markUserBought(productId, userId);

            log.info("订单已落库: token={}, orderId={}", token, order.getId());
        } catch (Exception e) {
            log.error("处理订单消息失败: message={}", message, e);
        }
    }

    // ==================== 订单查询 ====================

    /**
     * 根据令牌查询订单
     */
    public Optional<OrderResponse> getOrderByToken(String token) {
        return orderRepository.findByToken(token).map(this::toResponse);
    }

    private OrderResponse toResponse(SeckillOrder order) {
        OrderResponse resp = new OrderResponse();
        resp.setOrderId(order.getId());
        resp.setProductId(order.getProductId());
        resp.setUserId(order.getUserId());
        resp.setToken(order.getToken());
        resp.setStatus(order.getStatus());
        resp.setAmount(order.getAmount());
        resp.setCreateTime(order.getCreateTime());
        return resp;
    }

    // ==================== 工具方法 ====================

    /**
     * 从令牌生成订单 ID（取 token 的 hash 作为 ID 的一部分）
     */
    private Long generateOrderId(String token) {
        return Math.abs((long) token.hashCode()) + System.currentTimeMillis();
    }
}
