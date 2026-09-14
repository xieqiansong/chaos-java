package lan.chaos.rabbitmq.exchange;

import lan.chaos.rabbitmq.common.constant.MqConstants;
import lan.chaos.rabbitmq.common.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Headers 交换机：<b>按消息 header 匹配</b>，忽略 routing key。
 *
 * <p><b>机制：</b>绑定队列时声明 {@code where("type").equals("report")}，
 * 发布时通过 {@link MessagePostProcessor} 写入 header {@code type=report} 才会路由到该队列。
 * 适合「按属性而非按主题」路由（如类型 / 来源 / 版本）。
 * 本 demo 用 {@code where().equals()}（全部匹配）；也可用 {@code whereAny}（任一匹配）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeadersExchangeDemo {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 按 header 中的 type 值发布并同步拉回对应队列的消息。
     *
     * @param type header 值：{@code report} 或 {@code notify}
     */
    public OrderEvent publishTyped(String type, OrderEvent event) {
        MessagePostProcessor addHeader = message -> {
            message.getMessageProperties().getHeaders().put(MqConstants.HEADERS_MATCH_KEY, type);
            return message;
        };
        rabbitTemplate.convertAndSend(MqConstants.HEADERS_EXCHANGE, "", event, addHeader);
        log.info("[headers] 发布 orderId={} -> header {}={}", event.getOrderId(),
                MqConstants.HEADERS_MATCH_KEY, type);

        String queue = "report".equals(type)
                ? MqConstants.HEADERS_QUEUE_REPORT
                : MqConstants.HEADERS_QUEUE_NOTIFY;
        return (OrderEvent) rabbitTemplate.receiveAndConvert(queue, 3000L);
    }

    public OrderEvent receiveFromReport() {
        return (OrderEvent) rabbitTemplate.receiveAndConvert(MqConstants.HEADERS_QUEUE_REPORT, 2000L);
    }

    public OrderEvent receiveFromNotify() {
        return (OrderEvent) rabbitTemplate.receiveAndConvert(MqConstants.HEADERS_QUEUE_NOTIFY, 2000L);
    }
}
