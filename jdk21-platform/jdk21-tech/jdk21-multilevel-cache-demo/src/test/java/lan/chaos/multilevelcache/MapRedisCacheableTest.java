package lan.chaos.multilevelcache;

import lan.chaos.multilevelcache.cache.InMemoryBackend;
import lan.chaos.multilevelcache.cache.MapRedisCacheable;
import lan.chaos.multilevelcache.cache.VehicleSource;
import lan.chaos.multilevelcache.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M1 多级缓存单测：使用内存版 L2({@link InMemoryBackend})，无需 Redis 即可验证核心逻辑。
 */
class MapRedisCacheableTest {

    private MapRedisCacheable cache;

    @BeforeEach
    void setUp() {
        VehicleSource.INSTANCE.seed(1L, baseVehicle());
        cache = new MapRedisCacheable("VEHICLE", new InMemoryBackend(), 1000, 600);
    }

    private Vehicle baseVehicle() {
        return Vehicle.builder().vehicleId(1L).plateNo("京A·12345").status(1)
                .department("fleet-1").gpsLng("116.40").gpsLat("39.90").build();
    }

    @Test
    void shouldLoadFromSourceAndDoubleWriteOnFirstGet() {
        Vehicle v = cache.get(1L);
        assertNotNull(v);
        assertEquals("京A·12345", v.getPlateNo());
        // L1 已写入
        assertNotNull(cache.l1Cache().getIfPresent(1L));
    }

    @Test
    void shouldSkipNetworkWhenVersionMatches() {
        cache.get(1L); // 首次：回源+双写
        // 第二次：L1 命中且版本号一致，应直接返回本地值(不抛错、不回源)
        Vehicle v2 = cache.get(1L);
        assertSame(cache.l1Cache().getIfPresent(1L), v2);
    }

    @Test
    void shouldRefreshL1WhenValueChanged() {
        cache.get(1L);
        Vehicle moved = baseVehicle().toBuilder().gpsLng("116.99").build();
        cache.put(1L, moved); // 版本号变化 -> L2 版本自增
        Vehicle v = cache.get(1L);
        assertEquals("116.99", v.getGpsLng());
    }

    @Test
    void shouldInvalidateL1AfterRemove() {
        cache.get(1L);
        assertNotNull(cache.l1Cache().getIfPresent(1L));
        cache.remove(1L);
        // 删除后 L1 被清除；因种子数据仍在，再次 get 会回源重建
        assertNull(cache.l1Cache().getIfPresent(1L));
        Vehicle v = cache.get(1L);
        assertNotNull(v);
    }

    @Test
    void shouldReturnNullWhenAbsentAndNoSource() {
        // 未 seed 的 key，回源返回 null
        assertNull(cache.get(999L));
    }
}
