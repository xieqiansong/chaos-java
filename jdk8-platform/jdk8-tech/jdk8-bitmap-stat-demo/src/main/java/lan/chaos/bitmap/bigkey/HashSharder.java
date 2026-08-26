package lan.chaos.bitmap.bigkey;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * 哈希分片：crc32(deviceId) % shards。
 * 优点：数据均匀、无热点；代价：任何全局统计都要各片算完再汇总（跨片聚合由调用方做）。
 */
public final class HashSharder {

    private final int shards;

    public HashSharder(int shards) {
        if (shards <= 0) {
            throw new IllegalArgumentException("shards must be > 0, got " + shards);
        }
        this.shards = shards;
    }

    public int shards() {
        return shards;
    }

    public int shardOf(String deviceId) {
        CRC32 crc = new CRC32();
        crc.update(deviceId.getBytes(StandardCharsets.UTF_8));
        return (int) (crc.getValue() % shards);
    }
}
