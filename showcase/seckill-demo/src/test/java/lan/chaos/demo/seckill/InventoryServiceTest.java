package lan.chaos.demo.seckill;

import lan.chaos.demo.seckill.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InventoryService 单元测试
 * <p>
 * 验证 Redis Lua 库存扣减原子性和分桶策略的正确性。
 * 需要 Redis 服务运行中。
 */
@SpringBootTest
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final Long PRODUCT_ID = 99999L;
    private static final int TOTAL_STOCK = 100;
    private static final int BUCKET_COUNT = 5;

    @BeforeEach
    void setUp() {
        // 清理 Redis 中该商品的测试数据
        stringRedisTemplate.delete("seckill:stock:" + PRODUCT_ID);
        stringRedisTemplate.delete("seckill:sold_out:" + PRODUCT_ID);
        for (int i = 0; i < BUCKET_COUNT; i++) {
            stringRedisTemplate.delete("seckill:stock_bucket:" + PRODUCT_ID + ":" + i);
        }
    }

    @Test
    void testInitStock() {
        // 执行
        inventoryService.initStock(PRODUCT_ID, TOTAL_STOCK, BUCKET_COUNT);

        // 验证总库存
        assertEquals(TOTAL_STOCK, inventoryService.getRemainingStock(PRODUCT_ID));

        // 验证各分桶库存之和 = 总库存
        int bucketSum = 0;
        for (int i = 0; i < BUCKET_COUNT; i++) {
            bucketSum += inventoryService.getBucketStock(PRODUCT_ID, i);
        }
        assertEquals(TOTAL_STOCK, bucketSum);

        // 验证未被标记售罄
        assertFalse(inventoryService.isSoldOut(PRODUCT_ID));
    }

    @Test
    void testDeductBucketStock_Success() {
        // 准备
        inventoryService.initStock(PRODUCT_ID, TOTAL_STOCK, BUCKET_COUNT);

        // 执行首次扣减
        int result = inventoryService.deductBucketStock(PRODUCT_ID, 1, BUCKET_COUNT);

        // 验证扣减成功（返回值 > 0 表示桶索引）
        assertTrue(result > 0, "扣减应返回有效的桶索引");

        // 验证总库存减少
        assertEquals(TOTAL_STOCK - 1, inventoryService.getRemainingStock(PRODUCT_ID));
    }

    @Test
    void testDeductBucketStock_NoOversell() {
        // 准备：初始化极低库存测试超卖防护
        inventoryService.initStock(PRODUCT_ID, 3, BUCKET_COUNT);

        // 执行：尝试扣减 5 次（超过库存 3）
        int successCount = 0;
        for (int i = 0; i < 5; i++) {
            int result = inventoryService.deductBucketStock(PRODUCT_ID, 1, BUCKET_COUNT);
            if (result > 0) {
                successCount++;
            }
        }

        // 验证：成功扣减次数 = 总库存 3 次，第 4、5 次应返回 -1
        assertEquals(3, successCount, "扣减成功次数应等于总库存");

        // 验证：售罄标记已设置
        assertTrue(inventoryService.isSoldOut(PRODUCT_ID));

        // 验证：总库存为 0
        assertEquals(0, inventoryService.getRemainingStock(PRODUCT_ID));
    }

    @Test
    void testRollbackStock() {
        // 准备
        inventoryService.initStock(PRODUCT_ID, TOTAL_STOCK, BUCKET_COUNT);
        int bucketIndex = inventoryService.deductBucketStock(PRODUCT_ID, 1, BUCKET_COUNT);

        // 执行回滚
        inventoryService.rollbackStock(PRODUCT_ID, bucketIndex - 1, 1);

        // 验证库存已恢复
        assertEquals(TOTAL_STOCK, inventoryService.getRemainingStock(PRODUCT_ID));
    }

    @Test
    void testSoldOutFlag() {
        // 准备
        inventoryService.initStock(PRODUCT_ID, 1, 1);

        // 执行：仅有的 1 件被扣走
        inventoryService.deductBucketStock(PRODUCT_ID, 1, 1);

        // 验证：售罄
        assertTrue(inventoryService.isSoldOut(PRODUCT_ID));

        // 验证：再扣减返回 -1
        int result = inventoryService.deductBucketStock(PRODUCT_ID, 1, 1);
        assertEquals(-1, result);
    }
}
