package lan.chaos.rabbitmq.reliability;

import lan.chaos.rabbitmq.common.constant.MqConstants;
import lan.chaos.rabbitmq.common.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 生产者确认（Publisher Confirm）：保证消息 <b>已到达 Broker</b>。
 *
 * <p><b>机制：</b>开启 {@code publisherConfirms=true} 后，每条消息有 deliveryTag，
 * Broker 落盘/路由成功后回发 confirm。发布后调用 {@link RabbitTemplate#waitForConfirms(long)}
 * 阻塞等待全部 confirm，返回是否全部确认成功。</p>
 *
 * <p><b>与事务的区别：</b>confirm 是异步轻量确认（吞吐高），事务是同步阻塞（强一致但慢）。
 * 注意：confirm 只保证「到 Broker」，不保证「被消费」；路由失败（无匹配队列）也会 confirm 成功
 * （若要感知不可路由，需配合 publisher-returns / mandatory）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublisherConfirmDemo {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布并等待 Broker 确认。
     *
     * @return true=消息已被 Broker 确认
     */
    public boolean publishWithConfirm(OrderEvent event) {
        rabbitTemplate.convertAndSend(
                MqConstants.CONFIRM_EXCHANGE, MqConstants.CONFIRM_ROUTING, event);
        log.info("[confirm] 发布 orderId={} 并等待 Broker 确认", event.getOrderId());
        try {
            return rabbitTemplate.waitForConfirms(5000);
        } catch (Exception e) {
            log.error("[confirm] 等待确认超时/异常", e);
            return false;
        }
    }
}
