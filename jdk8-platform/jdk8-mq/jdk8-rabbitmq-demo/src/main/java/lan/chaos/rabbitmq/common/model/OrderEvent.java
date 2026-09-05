package lan.chaos.rabbitmq.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 演示实体：订单事件。
 *
 * <p>所有场景共用，自带 {@link #sample(String)} 工厂造默认数据，调用方无需自己准备输入。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {

    private String orderId;
    private String type;
    private String payload;
    private LocalDateTime createdAt;

    public static OrderEvent sample(String orderId) {
        return OrderEvent.builder()
                .orderId(orderId)
                .type("ORDER")
                .payload("sample-payload-" + orderId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
