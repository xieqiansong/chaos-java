package lan.chaos.multilevelcache;

import lan.chaos.multilevelcache.cache.MapRedisCacheable;
import lan.chaos.multilevelcache.cache.VehicleSource;
import lan.chaos.multilevelcache.model.Vehicle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时演示多级缓存核心行为：
 * 1. 首次读取 -> 回源(L2/DB) -> 双写(L2 + L1)。
 * 2. 二次读取 -> L1 命中且版本号一致 -> 跳过网络 IO 直接返回。
 * 3. 数据变更(版本号变化) -> 再次读取发生 L2 刷新并重建 L1。
 * 4. 删除 -> 版本号自增 -> 其他节点 L1 失效。
 */
@Slf4j
@Component
public class DemoRunner implements ApplicationRunner {

    private final MapRedisCacheable vehicleCache;

    public DemoRunner(MapRedisCacheable vehicleCache) {
        this.vehicleCache = vehicleCache;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 模拟 DB 初始数据
        VehicleSource.INSTANCE.seed(1L, Vehicle.builder()
                .vehicleId(1L).plateNo("京A·12345").status(1)
                .department("fleet-1").gpsLng("116.40").gpsLat("39.90").build());

        log.info("===== M1 多级缓存 Demo 开始 =====");

        // 1. 首次读取：L1 未命中 -> 回源 L2/DB -> 双写
        Vehicle v1 = vehicleCache.get(1L);
        log.info("[1] 首次读取(回源+双写): {}", v1.getPlateNo());

        // 2. 二次读取：L1 命中 + 版本号一致 -> 跳过网络
        Vehicle v2 = vehicleCache.get(1L);
        log.info("[2] 二次读取(L1 命中, 跳过网络): {}", v2.getPlateNo());

        // 3. 模拟 GPS 变化(值变化 -> 版本号变化) -> 重新写入 -> 再读触发 L2 刷新
        Vehicle moved = v1.toBuilder().gpsLng("116.41").gpsLat("39.91").build();
        vehicleCache.put(1L, moved);
        Vehicle v3 = vehicleCache.get(1L);
        log.info("[3] 数据变更后读取(L2 刷新 L1): gpsLng={}", v3.getGpsLng());

        // 4. 删除 -> 版本号自增 -> L1 失效，再次读取会回源
        vehicleCache.remove(1L);
        Vehicle v4 = vehicleCache.get(1L); // 因种子数据仍在，会再次回源
        log.info("[4] 删除后再次读取(回源): {}", v4 == null ? "null" : v4.getPlateNo());

        // 5. 批量读取演示 L1 聚合
        vehicleCache.get(1L);
        log.info("[5] L1 当前条目数: {}", vehicleCache.l1Cache().estimatedSize());

        log.info("===== M1 多级缓存 Demo 结束 =====");
    }
}
