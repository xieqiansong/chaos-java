package lan.chaos.finance.common.store;

import lan.chaos.finance.common.model.Position;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 机构头寸内存存储（演示用）。生产替换为数据库行锁版本，接口不变。 */
public class PositionStore {
    private final Map<String, Position> map = new ConcurrentHashMap<>();

    public Position save(Position position) {
        map.put(position.getInstitution(), position);
        return position;
    }

    public Position get(String institution) {
        Position position = map.get(institution);
        if (position == null) {
            throw new IllegalArgumentException("机构头寸不存在: " + institution);
        }
        return position;
    }

    public Collection<Position> all() {
        return map.values();
    }
}
