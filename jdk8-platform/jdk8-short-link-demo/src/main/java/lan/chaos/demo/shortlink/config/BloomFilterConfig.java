package lan.chaos.demo.shortlink.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置
 * 用于快速判断短链是否存在，避免缓存穿透
 */
@Configuration
public class BloomFilterConfig {

    private static final Logger log = LoggerFactory.getLogger(BloomFilterConfig.class);

    /** 预计元素数量 */
    private static final long EXPECTED_INSERTIONS = 10_000_000L;
    /** 期望误判率 */
    private static final double FPP = 0.01;

    @Bean
    public RBloomFilter<String> shortKeyBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("short-key-bloom");
        // 初始化布隆过滤器（仅首次需要）
        bloomFilter.tryInit(EXPECTED_INSERTIONS, FPP);
        log.info("Bloom filter initialized: expectedInsertions={}, fpp={}", EXPECTED_INSERTIONS, FPP);
        return bloomFilter;
    }
}
