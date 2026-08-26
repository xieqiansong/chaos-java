package lan.chaos.multilevelcache.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lan.chaos.multilevelcache.model.Vehicle;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 车辆多级缓存：具体实现，对应本模块多级缓存模板的车辆落地。
 *
 * <p>要点：
 * <ul>
 *   <li>L1 用 <b>Caffeine</b> 替换朴素 {@code ConcurrentHashMap}，
 *       具备 maximumSize 容量上限与 expireAfterWrite TTL，避免本地内存无界增长。</li>
 *   <li>L2 采用 Redis Hash + VERSION 版本号机制(可退化为内存版)。</li>
 * </ul>
 */
public class MapRedisCacheable extends AbstractMultilevelCacheable<Long, Vehicle> {

    public MapRedisCacheable(String bizKey,
                             CacheBackend backend,
                             long maximumSize,
                             long expireAfterWriteSeconds) {
        super(bizKey, backend, maximumSize, expireAfterWriteSeconds);
    }

    @Override
    protected Map<String, String> toHash(Vehicle value) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        map.put("vehicleId", String.valueOf(value.getVehicleId()));
        map.put("plateNo", value.getPlateNo());
        map.put("status", String.valueOf(value.getStatus()));
        map.put("department", value.getDepartment());
        map.put("gpsLng", value.getGpsLng());
        map.put("gpsLat", value.getGpsLat());
        return map;
    }

    @Override
    protected Vehicle fromHash(Map<String, String> hash) {
        return Vehicle.builder()
                .vehicleId(Long.valueOf(hash.get("vehicleId")))
                .plateNo(hash.get("plateNo"))
                .status(Integer.valueOf(hash.get("status")))
                .department(hash.get("department"))
                .gpsLng(hash.get("gpsLng"))
                .gpsLat(hash.get("gpsLat"))
                .build();
    }

    @Override
    protected Vehicle loadFromSource(Long key) {
        // 真实场景这里查 DB / 远程服务；Demo 用一个内存数据源模拟。
        return VehicleSource.INSTANCE.load(key);
    }

    /** 暴露 L1(Caffeine) 给演示代码，用于观察命中率与本地条目数。 */
    public Cache<Long, Vehicle> l1Cache() {
        return localCache;
    }
}
