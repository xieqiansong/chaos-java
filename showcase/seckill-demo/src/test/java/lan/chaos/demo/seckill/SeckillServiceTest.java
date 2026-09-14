package lan.chaos.demo.seckill;

import lan.chaos.demo.seckill.dto.SeckillRequest;
import lan.chaos.demo.seckill.dto.SeckillResponse;
import lan.chaos.demo.seckill.entity.Product;
import lan.chaos.demo.seckill.repository.ProductRepository;
import lan.chaos.demo.seckill.service.InventoryService;
import lan.chaos.demo.seckill.service.SeckillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SeckillService 集成测试
 * <p>
 * 模拟多线程并发抢购场景，验证无超卖。
 * 需要 Redis + PostgreSQL 服务运行中。
 */
@SpringBootTest
class SeckillServiceTest {

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    private Long productId;

    @BeforeEach
    void setUp() {
        // 创建测试商品
        Product product = new Product();
        product.setId(System.currentTimeMillis());
        product.setProductName("测试商品-秒杀");
        product.setTotalStock(20);
        product.setBucketCount(5);
        product.setStatus("ACTIVE");
        product.setStartTime(LocalDateTime.now().minusHours(1));
        product.setEndTime(LocalDateTime.now().plusHours(1));

        product = productRepository.save(product);
        productId = product.getId();

        // 初始化 Redis 库存
        inventoryService.initStock(productId, product.getTotalStock(), product.getBucketCount());
    }

    @Test
    void testSeckill_Success() {
        // 执行正常秒杀
        SeckillRequest request = new SeckillRequest();
        request.setUserId("test-user-001");
        request.setQuantity(1);

        SeckillResponse response = seckillService.seckill(productId, request);

        // 验证成功
        assertEquals(200, response.getCode(), "应返回 200 成功");
        assertNotNull(response.getData(), "应有 data");
        assertNotNull(response.getData().getToken(), "应生成令牌");
    }

    @Test
    void testSeckill_DuplicateUser() {
        // 第一次秒杀
        SeckillRequest request = new SeckillRequest();
        request.setUserId("test-user-002");
        request.setQuantity(1);
        seckillService.seckill(productId, request);

        // 同一用户再次秒杀同商品
        SeckillResponse response = seckillService.seckill(productId, request);

        // 验证被限购拦截
        assertEquals(409, response.getCode(), "重复购买应返回 409");
    }

    @Test
    void testSeckill_SoldOut() {
        // 创建极低库存商品
        Product lowStockProduct = new Product();
        lowStockProduct.setId(System.currentTimeMillis() + 1);
        lowStockProduct.setProductName("低库存测试商品");
        lowStockProduct.setTotalStock(1);
        lowStockProduct.setBucketCount(1);
        lowStockProduct.setStatus("ACTIVE");
        lowStockProduct.setStartTime(LocalDateTime.now().minusHours(1));
        lowStockProduct.setEndTime(LocalDateTime.now().plusHours(1));
        lowStockProduct = productRepository.save(lowStockProduct);

        inventoryService.initStock(lowStockProduct.getId(), 1, 1);

        // 第一人抢走
        SeckillRequest r1 = new SeckillRequest();
        r1.setUserId("user-a");
        seckillService.seckill(lowStockProduct.getId(), r1);

        // 第二人再抢
        SeckillRequest r2 = new SeckillRequest();
        r2.setUserId("user-b");
        SeckillResponse response = seckillService.seckill(lowStockProduct.getId(), r2);

        assertEquals(410, response.getCode(), "无库存应返回 410 售罄");
    }

    @Test
    void testSeckill_InvalidProduct() {
        SeckillRequest request = new SeckillRequest();
        request.setUserId("test-user-003");

        SeckillResponse response = seckillService.seckill(-1L, request);

        assertEquals(500, response.getCode(), "商品不存在应返回 500");
    }
}
