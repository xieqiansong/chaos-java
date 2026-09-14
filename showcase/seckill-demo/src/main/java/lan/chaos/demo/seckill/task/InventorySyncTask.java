package lan.chaos.demo.seckill.task;

import lan.chaos.demo.seckill.entity.Product;
import lan.chaos.demo.seckill.repository.ProductRepository;
import lan.chaos.demo.seckill.repository.SeckillOrderRepository;
import lan.chaos.demo.seckill.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 库存一致性对账定时任务
 * <p>
 * 定期对比 Redis 库存余量与数据库已售订单数，自动修复不一致。
 * 这是防超卖的最后一层保障（最终一致性）。
 */
@Component
public class InventorySyncTask {

    private static final Logger log = LoggerFactory.getLogger(InventorySyncTask.class);

    private final ProductRepository productRepository;
    private final SeckillOrderRepository orderRepository;
    private final InventoryService inventoryService;

    public InventorySyncTask(ProductRepository productRepository,
                             SeckillOrderRepository orderRepository,
                             InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * 每 30 秒执行一次库存对账
     */
    @Scheduled(fixedRate = 30000)
    public void syncInventory() {
        List<Product> activeProducts = productRepository.findByStatus("ACTIVE");

        for (Product product : activeProducts) {
            try {
                reconcile(product);
            } catch (Exception e) {
                log.error("库存对账异常: productId={}", product.getId(), e);
            }
        }
    }

    /**
     * 对账逻辑：Redis 余量 vs DB 已售数量
     */
    private void reconcile(Product product) {
        Long productId = product.getId();

        // 1. 计算 Redis 当前总余量（汇总所有分桶）
        int redisTotal = 0;
        for (int i = 0; i < product.getBucketCount(); i++) {
            redisTotal += inventoryService.getBucketStock(productId, i);
        }

        // 2. 计算 DB 已售订单数
        long dbSoldCount = orderRepository.countByUserIdAndProductIdAndStatus(
                "%", productId, "CONFIRMED");

        // 3. 计算期望 Redis 余量 = 总库存 - 已售
        int expectedRemaining = product.getTotalStock() - (int) dbSoldCount;

        if (expectedRemaining < 0) {
            expectedRemaining = 0;
        }

        // 4. 对比，若不一致则修复
        if (redisTotal != expectedRemaining) {
            log.warn("库存不一致: productId={}, Redis总余量={}, DB已售={}, 期望余量={}，开始修复",
                    productId, redisTotal, dbSoldCount, expectedRemaining);

            // 重新初始化 Redis 库存
            inventoryService.initStock(productId, expectedRemaining, product.getBucketCount());

            // 如果库存为 0，设置售罄标记
            if (expectedRemaining <= 0) {
                // 通过重新初始化已经清空了桶，标记已售罄
                product.setStatus("SOLD_OUT");
                productRepository.save(product);
            }

            log.info("库存修复完成: productId={}, 修复后余量={}", productId, expectedRemaining);
        }
    }

    /**
     * 每分钟检查一次"秒杀已结束"的商品，自动关闭
     */
    @Scheduled(fixedRate = 60000)
    public void closeExpiredSeckill() {
        List<Product> activeProducts = productRepository.findByStatus("ACTIVE");
        for (Product product : activeProducts) {
            if (!product.isInSeckillPeriod() && product.getEndTime() != null) {
                product.setStatus("CLOSED");
                productRepository.save(product);
                log.info("秒杀活动已过期关闭: productId={}, name={}", product.getId(), product.getProductName());
            }
        }
    }
}
