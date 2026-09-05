package lan.chaos.rocketmq.transaction;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 本地事务监听器。
 * <p>
 * 关键设计：抽出 {@link LocalTxStore} 抽象——{@code executeLocalTransaction} 写结果，
 * {@code checkLocalTransaction} 回查时"查库"得到最终状态——生产环境把 LocalTxStore 换成查询订单表即可，
 * 结构清晰、可持久化。
 */
@Slf4j
@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "rocketMQTemplate")
public class TransactionListener implements RocketMQLocalTransactionListener {

    @Resource
    private LocalTxStore localTxStore;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String orderId = (String) msg.getHeaders().get("orderId");
        log.info("1. 执行本地事务 | orderId={}", orderId);
        try {
            // 真实场景：insert 订单表。这里以 LocalTxStore 记录"落库结果"代替。
            localTxStore.mark(orderId, true);
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            localTxStore.mark(orderId, false);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String orderId = (String) msg.getHeaders().get("orderId");
        log.info("2. 事务回查（查库判定） | orderId={}", orderId);
        // 关键：回查时"查库"得到本地事务最终状态，而不是依赖易失的内存变量
        Boolean committed = localTxStore.get(orderId);
        if (committed == null) {
            return RocketMQLocalTransactionState.UNKNOWN; // 状态未知，Broker 稍后再次回查
        }
        return committed ? RocketMQLocalTransactionState.COMMIT : RocketMQLocalTransactionState.ROLLBACK;
    }
}
