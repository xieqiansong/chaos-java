package lan.chaos.bitmap.bigkey;

/**
 * 区域分片：设备 ID 前缀（地市/区域码）映射到分片，统计即分区。
 * 优点：单区域统计直接命中单片、零聚合（光猫场景大量报表本来按区域出）；
 * 代价：全局统计仍需跨片聚合，且区域间数据量可能不均（与哈希分片的取舍）。
 */
public final class RegionSharder {

    private final String[] regionPrefixes; // 分片下标 = 数组下标

    public RegionSharder(String[] regionPrefixes) {
        if (regionPrefixes == null || regionPrefixes.length == 0) {
            throw new IllegalArgumentException("regionPrefixes must not be empty");
        }
        this.regionPrefixes = regionPrefixes;
    }

    public int shards() {
        return regionPrefixes.length;
    }

    public int shardOf(String deviceId) {
        for (int i = 0; i < regionPrefixes.length; i++) {
            if (deviceId.startsWith(regionPrefixes[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("no region prefix matched: " + deviceId);
    }

    public String regionOf(int shard) {
        return regionPrefixes[shard];
    }
}
