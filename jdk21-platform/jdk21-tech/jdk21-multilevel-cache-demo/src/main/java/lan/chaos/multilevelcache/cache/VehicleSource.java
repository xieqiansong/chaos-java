package lan.chaos.multilevelcache.cache;

import lan.chaos.multilevelcache.model.Vehicle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 演示用内存数据源(模拟 DB / 远程服务)。
 * 真实项目里回源逻辑应查数据库或远程接口，这里用本地 Map 充当，便于开箱即跑。
 */
public enum VehicleSource {
    INSTANCE;

    private final Map<Long, Vehicle> db = new ConcurrentHashMap<>();

    /** 注入种子数据(模拟 DB 已有记录)。 */
    public void seed(Long id, Vehicle v) {
        db.put(id, v);
    }

    /** 回源加载。 */
    public Vehicle load(Long id) {
        return db.get(id);
    }
}
