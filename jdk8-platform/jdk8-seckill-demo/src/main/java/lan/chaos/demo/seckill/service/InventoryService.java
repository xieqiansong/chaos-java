package lan.chaos.demo.seckill.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 库存服务
 * <p>
 * 核心职责：
 * 1. Redis 库存初始化（分桶）
 * 2. Lua 脚本原子扣减库存（防超卖）
 * 3. 库存回滚
 * 4. 售罄标记管理
 */
@Service
public class InventoryService {

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String BUCKET_KEY_PREFIX = "seckill:stock_bucket:";
    private static final String SOLD_OUT_KEY_PREFIX = "seckill:sold_out:";

    private final StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> stockBucketScript;
    private DefaultRedisScript<Long> stockScript;

    public InventoryService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        // 加载分桶扣减 Lua 脚本
        stockBucketScript = new DefaultRedisScript<>();
        stockBucketScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/stock_bucket.lua")));
        stockBucketScript.setResultType(Long.class);

        // 加载简单扣减 Lua 脚本
        stockScript = new DefaultRedisScript<>();
        stockScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/stock.lua")));
        stockScript.setResultType(Long.class);
    }

    // ==================== Key 构建 ====================

    private String stockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    private String bucketKey(Long productId, int index) {
        return BUCKET_KEY_PREFIX + productId + ":" + index;
    }

    private String soldOutKey(Long productId) {
        return SOLD_OUT_KEY_PREFIX + productId;
    }

    // ==================== 库存初始化 ====================

    /**
     * 初始化商品库存到 Redis（分桶存储）
     *
     * @param productId   商品 ID
     * @param totalStock  总库存
     * @param bucketCount 分桶数量
     */
    public void initStock(Long productId, int totalStock, int bucketCount) {
        if (bucketCount <= 0) {
            bucketCount = 1;
        }
        int baseSize = totalStock / bucketCount;
        int remainder = totalStock % bucketCount;

        for (int i = 0; i < bucketCount; i++) {
            // 将余数分配到前几个桶
            int bucketStock = baseSize + (i < remainder ? 1 : 0);
            if (bucketStock > 0) {
                stringRedisTemplate.opsForValue().set(bucketKey(productId, i), String.valueOf(bucketStock));
            }
        }

        // 设置总库存 key（对账用）
        stringRedisTemplate.opsForValue().set(stockKey(productId), String.valueOf(totalStock));

        // 清除售罄标记
        stringRedisTemplate.delete(soldOutKey(productId));
    }

    // ==================== 库存扣减（核心防超卖） ====================

    /**
     * 分桶库存扣减（推荐）
     * <p>
     * 遍历所有分桶，依次尝试扣减，降低单 Key 热点竞争。
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     * @return 扣减成功的桶索引（从 1 开始），-1=已售罄
     */
    public int deductBucketStock(Long productId, int quantity, int bucketCount) {
        // 构建 keys: [soldOutKey, bucketKey0, bucketKey1, ...]
        List<String> keys = new ArrayList<>();
        keys.add(soldOutKey(productId));
        for (int i = 0; i < bucketCount; i++) {
            keys.add(bucketKey(productId, i));
        }

        Long result = stringRedisTemplate.execute(
                stockBucketScript,
                keys,
                String.valueOf(bucketCount),
                String.valueOf(quantity));

        return result != null ? result.intValue() : -1;
    }

    /**
     * 简单单 Key 库存扣减（备选方案）
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     * @return 扣减后的库存余量，-1=库存不足，-2=未初始化
     */
    public long deductSimpleStock(Long productId, int quantity) {
        Long result = stringRedisTemplate.execute(
                stockScript,
                new ArrayList<String>() {{
                    add(stockKey(productId));
                }},
                String.valueOf(quantity));

        return result != null ? result : -2;
    }

    // ==================== 库存回滚 ====================

    /**
     * 回滚指定分桶的库存
     *
     * @param productId   商品 ID
     * @param bucketIndex 桶索引
     * @param quantity    回滚数量
     */
    public void rollbackStock(Long productId, int bucketIndex, int quantity) {
        String key = bucketKey(productId, bucketIndex);
        stringRedisTemplate.opsForValue().increment(key, quantity);

        // 同时增加总库存（对账用）
        stringRedisTemplate.opsForValue().increment(stockKey(productId), quantity);

        // 清除售罄标记（如果有回滚说明又有库存了）
        stringRedisTemplate.delete(soldOutKey(productId));
    }

    // ==================== 查询 ====================

    /**
     * 获取商品剩余总库存
     */
    public int getRemainingStock(Long productId) {
        String val = stringRedisTemplate.opsForValue().get(stockKey(productId));
        return val != null ? Integer.parseInt(val) : 0;
    }

    /**
     * 检查商品是否已售罄
     */
    public boolean isSoldOut(Long productId) {
        String val = stringRedisTemplate.opsForValue().get(soldOutKey(productId));
        return "1".equals(val);
    }

    /**
     * 获取指定分桶余量
     */
    public int getBucketStock(Long productId, int bucketIndex) {
        String val = stringRedisTemplate.opsForValue().get(bucketKey(productId, bucketIndex));
        return val != null ? Integer.parseInt(val) : 0;
    }
}
