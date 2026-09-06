package lan.chaos.redis.stream;

import lan.chaos.redis.common.constant.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 消息队列（Redis 5+）★★☆。
 *
 * <p>相比 {@code pubsub}（无持久化、无 ACK，订阅者离线消息即丢失），Stream 是<b>持久化</b>的日志结构，
 * 支持 <b>消费组（Consumer Group）</b> 与 <b>消息确认（ACK）</b>，可承载可靠的消息队列场景
 * （替代 Pub/Sub 的无持久化短板）。</p>
 *
 * <p>核心命令映射：</p>
 * <ul>
 *     <li>{@code XADD} → {@link #produce(String, String)} 生产</li>
 *     <li>{@code XGROUP CREATE} → {@link #ensureGroup(String)} 建消费组（已存在则忽略）</li>
 *     <li>{@code XREADGROUP} → {@link #consume(String, String)} 组内消费（非阻塞）</li>
 *     <li>{@code XACK} → {@link #ack(String, RecordId)} 确认，移出 PEL（待确认列表）</li>
 *     <li>{@code XPENDING} → {@link #pending(String)} 查未确认概览（堆积监控）</li>
 * </ul>
 *
 * <p><b>坑点：</b>消费组基于 {@code latest} 创建时只投递「建组之后」的消息；建组前需先
 * {@link #produce(String, String)} 让 stream 存在（否则需 {@code MKSTREAM}）。务必 {@code ack}，
 * 否则消息长期堆积在 PEL 占用内存。</p>
 */
@Service
public class StreamService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 生产：往 stream 追加一条 field-value 消息，返回消息 ID（&lt;毫秒时间戳&gt;-&lt;序号&gt;） */
    public String produce(String field, String value) {
        Map<String, String> body = new HashMap<>();
        body.put(field, value);
        MapRecord<String, String, String> record =
                StreamRecords.newRecord().ofMap(body).withStreamKey(RedisKeyConstants.STREAM_KEY);
        return stringRedisTemplate.opsForStream().add(record).getValue();
    }

    /** 创建消费组（组已存在时忽略 BUSYGROUP 异常）。基于 latest，只投递建组后的新消息 */
    public void ensureGroup(String group) {
        try {
            stringRedisTemplate.opsForStream()
                    .createGroup(RedisKeyConstants.STREAM_KEY, ReadOffset.latest(), group);
        } catch (org.springframework.data.redis.RedisSystemException ignore) {
            // BUSYGROUP：消费组已存在，忽略
        }
    }

    /** 消费：组内某消费者读取尚未消费的消息（非阻塞，无消息返回空列表）。
     *  Spring Data Redis 2.7 用 {@code read(Consumer, StreamOffset...)} 对应 {@code XREADGROUP} */
    public List<MapRecord<String, Object, Object>> consume(String group, String consumer) {
        return stringRedisTemplate.opsForStream().read(
                Consumer.from(group, consumer),
                StreamOffset.create(RedisKeyConstants.STREAM_KEY, ReadOffset.lastConsumed()));
    }

    /** 确认：把消息标记为已处理，从 PEL 移除，避免内存堆积 */
    public long ack(String group, RecordId id) {
        return stringRedisTemplate.opsForStream()
                .acknowledge(RedisKeyConstants.STREAM_KEY, group, id);
    }

    /** 待确认概览：未 ACK 消息总数、首尾 ID、各消费者堆积 */
    public PendingMessagesSummary pending(String group) {
        return stringRedisTemplate.opsForStream()
                .pending(RedisKeyConstants.STREAM_KEY, group);
    }
}
