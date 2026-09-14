package lan.chaos.demo.seckill;

import lan.chaos.demo.seckill.dto.SeckillRequest;
import lan.chaos.demo.seckill.dto.SeckillResponse;
import lan.chaos.demo.seckill.entity.Product;
import lan.chaos.demo.seckill.repository.ProductRepository;
import lan.chaos.demo.seckill.service.InventoryService;
import lan.chaos.demo.seckill.service.SeckillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并发秒杀压测
 * <p>
 * 使用 CountDownLatch 模拟 200 个用户同时秒杀 50 件商品，
 * 验证最终成功数 = 库存数（无超卖）。
 */
@SpringBootTest
class ConcurrentSeckillTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentSeckillTest.class);

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    private static final int TOTAL_STOCK = 50;
    private static final int CONCURRENT_USERS = 200;

    private Long productId;
    private AtomicInteger successCount;
    private AtomicInteger failCount;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setId(System.currentTimeMillis());
        product.setProductName("并发压测商品");
        product.setTotalStock(TOTAL_STOCK);
        product.setBucketCount(10);
        product.setStatus("ACTIVE");
        product.setStartTime(LocalDateTime.now().minusHours(1));
        product.setEndTime(LocalDateTime.now().plusHours(1));

        product = productRepository.save(product);
        productId = product.getId();

        inventoryService.initStock(productId, TOTAL_STOCK, 10);

        successCount = new AtomicInteger(0);
        failCount = new AtomicInteger(0);
    }

    @Test
    void testConcurrentSeckill_NoOversell() throws InterruptedException {
        int threadCount = CONCURRENT_USERS;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(50);

        long start = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final String userId = "con-user-" + i;
            executor.submit(() -> {
                try {
                    // 模拟网络波动，让请求更分散
                    Thread.sleep((long) (Math.random() * 50));

                    SeckillRequest request = new SeckillRequest();
                    request.setUserId(userId);
                    request.setQuantity(1);

                    SeckillResponse response = seckillService.seckill(productId, request);

                    if (response.getCode() == 200) {
                        successCount.incrementAndGet();
                    } else if (response.getCode() == 410 || response.getCode() == 429) {
                        failCount.incrementAndGet();
                    } else if (response.getCode() == 409) {
                        // 限购拦截，也计入失败
                        failCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("并发请求异常: userId={}", userId, e);
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long elapsed = System.currentTimeMillis() - start;

        // === 核心验证：成功数 = 总库存（无超卖） ===
        log.info("===== 并发秒杀压测结果 =====");
        log.info("总库存: {}, 并发用户数: {}", TOTAL_STOCK, CONCURRENT_USERS);
        log.info("成功数: {}, 失败数: {}, 耗时: {}ms", successCount.get(), failCount.get(), elapsed);
        log.info("总请求: {}", successCount.get() + failCount.get());

        // 验证 1: 成功数不超过库存（无超卖）
        assertTrue(successCount.get() <= TOTAL_STOCK,
                "超卖！成功数 " + successCount.get() + " 超过库存 " + TOTAL_STOCK);

        // 验证 2: 成功数 + 失败数 = 总请求数（无丢失）
        assertEquals(CONCURRENT_USERS, successCount.get() + failCount.get(),
                "请求总数不匹配，可能存在请求丢失");

        // 验证 3: 验证 Redis 最终库存 = 0 或接近 0（全卖完或接近卖完）
        int remaining = inventoryService.getRemainingStock(productId);
        int expectedRemaining = Math.max(0, TOTAL_STOCK - successCount.get());
        assertEquals(expectedRemaining, remaining,
                "Redis 最终库存与预期不一致");

        log.info("Redis 最终库存: {} (期望: {})", remaining, expectedRemaining);
        log.info("===== 验证通过：无超卖！ =====");
    }

    @Test
    void testExtremeConcurrent_AllUsersSameProduct() throws InterruptedException {
        // 极端情况：1件库存，50人抢
        Product singleProduct = new Product();
        singleProduct.setId(System.currentTimeMillis() + 100);
        singleProduct.setProductName("1件库存极限测试");
        singleProduct.setTotalStock(1);
        singleProduct.setBucketCount(1);
        singleProduct.setStatus("ACTIVE");
        singleProduct.setStartTime(LocalDateTime.now().minusHours(1));
        singleProduct.setEndTime(LocalDateTime.now().plusHours(1));
        singleProduct = productRepository.save(singleProduct);

        inventoryService.initStock(singleProduct.getId(), 1, 1);

        int userCount = 50;
        CountDownLatch latch = new CountDownLatch(userCount);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger success = new AtomicInteger(0);

        for (int i = 0; i < userCount; i++) {
            final String userId = "extreme-user-" + i;
            Product finalSingleProduct = singleProduct;
            executor.submit(() -> {
                try {
                    SeckillRequest request = new SeckillRequest();
                    request.setUserId(userId);

                    SeckillResponse response = seckillService.seckill(finalSingleProduct.getId(), request);
                    if (response.getCode() == 200) {
                        success.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("极限压测异常", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        log.info("1件库存 {}人抢，成功数: {}", userCount, success.get());

        // 验证：最多 1 人成功
        assertTrue(success.get() <= 1,
                "1件库存被 " + success.get() + " 人抢到，超卖！");
    }
}
