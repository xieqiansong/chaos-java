package lan.chaos.rocketmq;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import lan.chaos.rocketmq.acl.AclProducer;
import lan.chaos.rocketmq.batch.BatchProducer;
import lan.chaos.rocketmq.broadcast.BroadcastNoRetryProducer;
import lan.chaos.rocketmq.broadcast.BroadcastProducer;
import lan.chaos.rocketmq.delay.DelayProducer;
import lan.chaos.rocketmq.faulttolerant.FaultTolerantProducer;
import lan.chaos.rocketmq.filter.FilterProducer;
import lan.chaos.rocketmq.keyquery.KeyQueryProducer;
import lan.chaos.rocketmq.order.GlobalOrderProducer;
import lan.chaos.rocketmq.order.OrderedProducer;
import lan.chaos.rocketmq.pull.PullConsumer;
import lan.chaos.rocketmq.pull.PullProducer;
import lan.chaos.rocketmq.requestreply.RequestReplyProducer;
import lan.chaos.rocketmq.retry.RetryProducer;
import lan.chaos.rocketmq.simple.SimpleProducer;
import lan.chaos.rocketmq.throttle.ThrottleProducer;
import lan.chaos.rocketmq.trace.TraceProducer;
import lan.chaos.rocketmq.transaction.TransactionProducer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * RocketMQ 演示统一触发入口。
 * <p>
 * 需要可用的 RocketMQ（application.yml: REDACTED:9876）才能真正跑通；
 * 每个用例发送后用 sleep 等待消费端打印结果。单独运行某个 @Test 方法即可演示对应场景。
 */
@SpringBootTest(classes = App.class)
@Execution(ExecutionMode.CONCURRENT)
@Disabled
class DemoTest {
    static int DELAY_SECOND = 2000;

    @Test
    void sync() {
        SpringUtil.getBean(SimpleProducer.class).sendSync("测试同步消息");
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void async() {
        SpringUtil.getBean(SimpleProducer.class).sendAsync("测试异步消息");
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void oneWay() {
        SpringUtil.getBean(SimpleProducer.class).sendOneWay("测试单向消息");
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void order() {
        SpringUtil.getBean(OrderedProducer.class).sendOrderLifecycle("ORDER_001");
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void retry() {
        // body 含 error 触发消费失败 -> 重试 3 次 -> 进死信
        SpringUtil.getBean(RetryProducer.class).send("user_error_" + System.currentTimeMillis());
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void tx() {
        SpringUtil.getBean(TransactionProducer.class).sendOrderTransactionMsg("ORDER_" + System.currentTimeMillis());
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void delay() {
        SpringUtil.getBean(DelayProducer.class).sendDelay("delay-test", 1);
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void batch() {
        SpringUtil.getBean(BatchProducer.class).sendBatch();
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void filter() {
        SpringUtil.getBean(FilterProducer.class).send();
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void broadcast() {
        SpringUtil.getBean(BroadcastProducer.class).send("广播模式消息");
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void requestReply() {
        SpringUtil.getBean(RequestReplyProducer.class).send();
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void pull() {
        SpringUtil.getBean(PullProducer.class).send();
        ThreadUtil.sleep(DELAY_SECOND);
        SpringUtil.getBean(PullConsumer.class).demo();
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void globalOrder() {
        SpringUtil.getBean(GlobalOrderProducer.class).send();
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void keyQuery() {
        String key = "order-1001";
        SpringUtil.getBean(KeyQueryProducer.class).sendWithKey(key, "订单创建消息");
        ThreadUtil.sleep(DELAY_SECOND);
        SpringUtil.getBean(KeyQueryProducer.class).queryByKey(key);
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void faultTolerant() {
        SpringUtil.getBean(FaultTolerantProducer.class).send("发送侧容错参数演示");
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void trace() {
        SpringUtil.getBean(TraceProducer.class).send("消息轨迹演示");
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void acl() {
        SpringUtil.getBean(AclProducer.class).send("ACL 鉴权演示");
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void throttle() {
        SpringUtil.getBean(ThrottleProducer.class).sendBatch(20);
        ThreadUtil.sleep(DELAY_SECOND);
    }

    @Test
    void broadcastNoRetry() {
        SpringUtil.getBean(BroadcastNoRetryProducer.class).send();
        ThreadUtil.sleep(DELAY_SECOND);
    }
}
