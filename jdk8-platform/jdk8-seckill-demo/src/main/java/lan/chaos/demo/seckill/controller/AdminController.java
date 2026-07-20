package lan.chaos.demo.seckill.controller;

import lan.chaos.demo.seckill.entity.Product;
import lan.chaos.demo.seckill.service.InventoryService;
import lan.chaos.demo.seckill.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理控制器
 * <p>
 * 用于创建秒杀商品、初始化库存、查询状态等管理操作
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ProductService productService;
    private final InventoryService inventoryService;

    public AdminController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    /**
     * 创建秒杀商品（含库存初始化）
     * <p>
     * POST /api/admin/product
     * {
     * "productName": "iPhone 16 Pro",
     * "totalStock": 1000,
     * "bucketCount": 10,
     * "price": 8999.00,
     * "startTime": "2026-07-02T10:00:00",
     * "endTime": "2026-07-02T11:00:00"
     * }
     */
    @PostMapping("/product")
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> params) {
        try {
            Product product = new Product();
            product.setId(System.currentTimeMillis());
            product.setProductName((String) params.get("productName"));
            product.setTotalStock((Integer) params.get("totalStock"));
            product.setBucketCount(params.get("bucketCount") != null
                    ? (Integer) params.get("bucketCount") : 10);
            product.setPrice(params.get("price") != null
                    ? new BigDecimal(params.get("price").toString()) : BigDecimal.ZERO);
            product.setStatus("DRAFT");

            if (params.get("startTime") != null) {
                product.setStartTime(LocalDateTime.parse((String) params.get("startTime")));
            }
            if (params.get("endTime") != null) {
                product.setEndTime(LocalDateTime.parse((String) params.get("endTime")));
            }

            // 先保存到数据库获取 ID
            product = productService.createProduct(product);

            // 初始化 Redis 库存（分桶）
            inventoryService.initStock(product.getId(), product.getTotalStock(), product.getBucketCount());

            log.info("商品创建成功: id={}, name={}, stock={}, buckets={}",
                    product.getId(), product.getProductName(),
                    product.getTotalStock(), product.getBucketCount());

            Map<String, Object> result = new HashMap<>();
            result.put("id", product.getId());
            result.put("productName", product.getProductName());
            result.put("totalStock", product.getTotalStock());
            result.put("bucketCount", product.getBucketCount());
            result.put("status", product.getStatus());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建商品失败", e);
            return ResponseEntity.status(500).body("创建失败: " + e.getMessage());
        }
    }

    /**
     * 启动秒杀活动
     * <p>
     * POST /api/admin/product/{productId}/activate
     */
    @PostMapping("/product/{productId}/activate")
    public ResponseEntity<?> activateProduct(@PathVariable Long productId) {
        try {
            Product product = productService.activateSeckill(productId);
            log.info("秒杀活动已启动: productId={}", productId);
            return ResponseEntity.ok("秒杀活动已启动");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("启动失败: " + e.getMessage());
        }
    }

    /**
     * 查询商品库存状态
     * <p>
     * GET /api/admin/product/{productId}/stock
     */
    @GetMapping("/product/{productId}/stock")
    public ResponseEntity<?> getStock(@PathVariable Long productId) {
        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("remainingStock", inventoryService.getRemainingStock(productId));
        result.put("soldOut", inventoryService.isSoldOut(productId));
        result.put("currentQps", 0); // 可额外查询限流统计
        return ResponseEntity.ok(result);
    }
}
