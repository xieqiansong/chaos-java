package lan.chaos.demo.seckill.service;

import lan.chaos.demo.seckill.dto.SeckillRequest;
import lan.chaos.demo.seckill.dto.SeckillResponse;
import lan.chaos.demo.seckill.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * 秒杀核心服务
 * <p>
 * 秒杀主流程：限流检查 → 商品校验 → 限购检查 → 库存预扣 → 生成令牌 → 异步下单
 */
@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    private final RateLimitService rateLimitService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final OrderService orderService;

    public SeckillService(RateLimitService rateLimitService,
                          ProductService productService,
                          InventoryService inventoryService,
                          OrderService orderService) {
        this.rateLimitService = rateLimitService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.orderService = orderService;
    }

    /**
     * 秒杀主流程
     *
     * @param productId 商品 ID
     * @param request   秒杀请求
     * @return 秒杀响应
     */
    public SeckillResponse seckill(Long productId, SeckillRequest request) {
        // 1. 令牌桶限流
        if (!rateLimitService.tryAcquire(productId)) {
            log.warn("限流拦截: userId={}, productId={}", request.getUserId(), productId);
            return SeckillResponse.rateLimited();
        }

        // 2. 查询商品信息（本地缓存）
        Optional<Product> productOpt = productService.getProduct(productId);
        if (!productOpt.isPresent()) {
            return SeckillResponse.error("商品不存在");
        }
        Product product = productOpt.get();

        // 3. 校验秒杀活动是否进行中
        if (!product.isInSeckillPeriod()) {
            return SeckillResponse.error("秒杀活动未开始或已结束");
        }

        // 4. 检查是否已售罄（Redis 快速过滤）
        if (inventoryService.isSoldOut(productId)) {
            log.info("售罄拦截: userId={}, productId={}", request.getUserId(), productId);
            return SeckillResponse.soldOut();
        }

        // 5. 限购检查 — 每人限购1件
        if (orderService.hasUserBought(productId, request.getUserId())) {
            log.info("限购拦截: userId={}, productId={}", request.getUserId(), productId);
            return SeckillResponse.duplicate();
        }

        int quantity = request.getQuantity() != null && request.getQuantity() > 0
                ? request.getQuantity() : 1;

        // 6. Redis Lua 原子扣减库存（分桶）
        int bucketIndex = inventoryService.deductBucketStock(
                productId, quantity, product.getBucketCount());

        if (bucketIndex < 0) {
            log.info("库存不足: userId={}, productId={}", request.getUserId(), productId);
            return SeckillResponse.soldOut();
        }

        // 6.5 同步标记用户已购（幂等），保证限购检查确定性，不依赖异步下单时序
        orderService.markUserBought(productId, request.getUserId());

        // 7. 生成唯一令牌
        String token = UUID.randomUUID().toString().replace("-", "");

        // 8. 异步发送订单消息
        try {
            // 简单订单 ID（生产环境使用雪花算法）
            long orderId = generateOrderId(token);
            BigDecimal amount = product.getPrice() != null
                    ? product.getPrice().multiply(BigDecimal.valueOf(quantity))
                    : BigDecimal.ZERO;

            orderService.asyncCreateOrder(orderId, productId, request.getUserId(),
                    token, bucketIndex, amount);

            log.info("秒杀成功: userId={}, productId={}, token={}, bucket={}",
                    request.getUserId(), productId, token, bucketIndex);

            return SeckillResponse.success(token, orderId);
        } catch (Exception e) {
            // 9. 异常回滚 — 异步下单失败，回滚库存
            log.error("异步下单失败，回滚库存: bucketIndex={}", bucketIndex, e);
            inventoryService.rollbackStock(productId, bucketIndex, quantity);
            return SeckillResponse.error("系统繁忙，请重试");
        }
    }

    /**
     * 生成订单 ID（简易版，生产环境使用雪花算法）
     */
    private long generateOrderId(String token) {
        return Math.abs((long) token.hashCode()) + System.currentTimeMillis();
    }
}
