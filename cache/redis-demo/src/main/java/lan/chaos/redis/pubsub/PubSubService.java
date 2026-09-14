package lan.chaos.redis.pubsub;

import lan.chaos.redis.common.constant.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 发布订阅（Pub/Sub）★★☆。
 *
 * <p><b>为什么用 Pub/Sub：</b>轻量广播、事件通知，发布即达订阅者。</p>
 *
 * <p><b>坑点：</b>Pub/Sub 无持久化、无 ACK，订阅者离线期间的消息会丢失；
 * 需要可靠消息请用 Stream（Redis 5+）。</p>
 *
 * <p><b>可观测：</b>订阅端收到消息后由 {@code PubSubConfig} 的监听器写入 {@link #recent}，
 * 调用 {@link #recent()} 即可查看最近收到的消息，无需盯控制台。</p>
 */
@Service
public class PubSubService {

    /** 最近收到的消息（最多保留 10 条），用于 HTTP 可观测 */
    private final List<String> recent = Collections.synchronizedList(new LinkedList<>());

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void publish(String message) {
        stringRedisTemplate.convertAndSend(RedisKeyConstants.PUBSUB_CHANNEL, message);
    }

    /** 订阅端回调：记录消息供 HTTP 查询 */
    public void record(String message) {
        recent.add(message);
        if (recent.size() > 10) {
            recent.remove(0);
        }
    }

    public List<String> recent() {
        return new LinkedList<>(recent);
    }
}
