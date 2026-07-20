package lan.chaos.kafka.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单事件——用于有序、事务等场景的消息模型。
 *
 * <p><b>有序场景：</b>{@link #orderId} 作为 Kafka 的 key，
 * 保证同一订单的所有事件路由到同一 partition、消费有序。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    /** 订单号（分区路由 key） */
    private String orderId;
    /** 操作类型（CREATE/PAY/SHIP/DONE） */
    private String action;
    /** 可选备注 */
    private String remark;
}
