package lan.chaos.localcache;

import lan.chaos.localcache.basic.BasicCacheService;
import lan.chaos.localcache.cacheaside.CacheAsideService;
import lan.chaos.localcache.common.model.User;
import lan.chaos.localcache.eviction.EvictionCacheService;
import lan.chaos.localcache.expire.ExpireCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地缓存标杆 Demo 的核心验证：每个能力一条可断言的测试。
 * 纯内存、零外部依赖，无需 Assumptions 跳过，任何环境（含 CI）直接跑。
 */
@SpringBootTest
class LocalCacheScenarioTest {

    @Autowired
    private BasicCacheService basic;
    @Autowired
    private ExpireCacheService expire;
    @Autowired
    private EvictionCacheService eviction;
    @Autowired
    private CacheAsideService cacheAside;

    @Test
    void basic_putThenGet_thenInvalidate() {
        User u = basic.putAndGet(1L);
        assertNotNull(u);
        assertEquals("user-1", u.getName());
        basic.invalidate(1L);
        assertNull(basic.get(1L));
    }

    @Test
    void expire_valueGoneAfterTtl() throws InterruptedException {
        expire.put(1L);
        assertNotNull(expire.get(1L));
        TimeUnit.SECONDS.sleep(3); // 等过 2s TTL（演示用短 TTL；生产为分钟级）
        assertNull(expire.get(1L));
    }

    @Test
    void eviction_keepsWithinMaximumSize() {
        for (long i = 1; i <= 5; i++) {
            eviction.put(i);
        }
        // 触发一次淘汰维护（生产由读写自动触发），size 应收敛到 ≤ maximumSize
        eviction.cache().cleanUp();
        assertTrue(eviction.cache().asMap().size() <= 3, "存活 key 不应超过 maximumSize=3");
        assertTrue(eviction.cache().stats().hitRate() >= 0.0);
    }

    @Test
    void cacheAside_secondCallHitsCache() {
        cacheAside.load(1L);
        cacheAside.load(1L);
        assertEquals(1, cacheAside.getDbCalls(), "同一 key 两次调用只应查库一次");
    }
}
