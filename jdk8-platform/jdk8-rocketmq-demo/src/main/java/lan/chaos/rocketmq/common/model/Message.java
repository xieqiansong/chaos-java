package lan.chaos.rocketmq.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一消息信封：所有场景的消息正文都包一层，
 * 携带 {@code body}（原始业务正文）与 {@code timestamp}（发送时刻毫秒）。
 * 消费者据此计算"消息从发送到被消费"的耗时。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /** 原始业务正文 */
    private String body;

    /** 发送时刻（System.currentTimeMillis()） */
    private long timestamp;
}
