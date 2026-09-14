package lan.chaos.redis.pipeline;

import lan.chaos.redis.common.constant.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pipeline 批量读写 ★★☆。
 *
 * <p><b>为什么用 Pipeline：</b>逐条 SET 每次都要一次网络往返（RTT），批量场景吞吐差。
 * Pipeline 把多条命令打包一次往返发送，大幅提升吞吐。</p>
 *
 * <p><b>坑点：</b>Pipeline 期间命令在客户端缓冲，批量过大可能撑爆内存或单次包超限，
 * 建议分批（如每 1000 条一管道）。Pipeline 不等同于事务，不保证原子。</p>
 */
@Service
public class PipelineService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void set(Map<String, String> kvs) {
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection conn = (StringRedisConnection) connection;
            kvs.forEach((k, v) -> conn.set(k, v));
            return null;
        });
    }

    public List<Object> get(List<String> keys) {
        return stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection conn = (StringRedisConnection) connection;
            keys.forEach(conn::get);
            return null;
        });
    }

    /** 演示：批量写入 5 条再读回，返回 key→value 映射 */
    public Map<String, Object> demo() {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            map.put(RedisKeyConstants.CACHE_KEY + "pipe:" + i, "v" + i);
        }
        set(map);
        List<String> keys = new ArrayList<>(map.keySet());
        List<Object> values = get(keys);
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            result.put(keys.get(i), values.get(i));
        }
        return result;
    }
}
