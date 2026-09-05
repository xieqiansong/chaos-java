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
import lan.chaos.rocketmq.reliability.ReliabilityProducer;
import lan.chaos.rocketmq.pull.PullConsumer;
import lan.chaos.rocketmq.pull.PullProducer;
import lan.chaos.rocketmq.requestreply.RequestReplyProducer;
import lan.chaos.rocketmq.retry.RetryProducer;
import lan.chaos.rocketmq.simple.SimpleProducer;
import lan.chaos.rocketmq.throttle.ThrottleProducer;
import lan.chaos.rocketmq.trace.TraceProducer;
import lan.chaos.rocketmq.transaction.TransactionProducer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * RocketMQ 场景统一触发入口。
 * <p>
 * 设计要点（对齐 AGENTS 规范）：
 * <ul>
 *     <li>每个场景一个独立 {@code @Test}，去掉了原先类级 {@code @Disabled}
 *         （禁止「整体禁用 + Thread.sleep 当入口」的历史包袱）；</li>
 *     <li>方法首行用 {@link Assumptions} 探测 NameServer 是否可达，
 *         不可达时该用例被<b>优雅跳过</b>（CI 无外部依赖时零误报，而非失败）；</li>
 *     <li>可达时真实发送，并用 {@link Assertions#assertDoesNotThrow} 断言链路不抛异常；</li>
 *     <li>末尾 {@code sleep} 仅用于等待异步消费端打印结果（可观察输出），并非测试入口。</li>
 * </ul>
 * 需要可用的 RocketMQ：先用 {@code docker-compose up -d} 起 NameServer + Broker（见本目录 docker-compose.yml）。
 * NameServer 地址与 application.yml 保持一致（默认 REDACTED:9876，可用 {@code -Drocketmq.namesrv.addr=HOST:PORT} 覆盖）。
 */
@SpringBootTest(classes = RocketMqApplication.class)
@Execution(ExecutionMode.CONCURRENT)
class DemoTest {
    static int WAIT_CONSUME_MS = 2000;

    @Test
    void sync() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(SimpleProducer.class).sendSync("测试同步消息"));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void async() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(SimpleProducer.class).sendAsync("测试异步消息"));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void oneWay() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(SimpleProducer.class).sendOneWay("测试单向消息"));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void order() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(OrderedProducer.class).sendOrderLifecycle("ORDER_001"));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void retry() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(RetryProducer.class).send("user_error_" + System.currentTimeMillis()));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void tx() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(TransactionProducer.class).sendOrderTransactionMsg("ORDER_" + System.currentTimeMillis()));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void delay() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(DelayProducer.class).sendDelay("delay-test", 1));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void batch() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(BatchProducer.class).sendBatch());
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void filter() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(FilterProducer.class).send());
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void broadcast() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(BroadcastProducer.class).send("广播模式消息"));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void requestReply() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(RequestReplyProducer.class).send());
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void pull() {
        Assertions.assertDoesNotThrow(() -> {
            SpringUtil.getBean(PullProducer.class).send();
            SpringUtil.getBean(PullConsumer.class).demo();
        });
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void globalOrder() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(GlobalOrderProducer.class).send());
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void keyQuery() {
        Assertions.assertDoesNotThrow(() -> {
            String key = "order-1001";
            SpringUtil.getBean(KeyQueryProducer.class).sendWithKey(key, "订单创建消息");
            SpringUtil.getBean(KeyQueryProducer.class).queryByKey(key);
        });
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void faultTolerant() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(FaultTolerantProducer.class).send("发送侧容错参数演示"));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void trace() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(TraceProducer.class).send("消息轨迹演示"));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void acl() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(AclProducer.class).send("ACL 鉴权演示"));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void throttle() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(ThrottleProducer.class).sendBatch(20));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void broadcastNoRetry() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(BroadcastNoRetryProducer.class).send());
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }

    @Test
    void reliability() {
        Assertions.assertDoesNotThrow(() ->
                SpringUtil.getBean(ReliabilityProducer.class).sendGuaranteed("REL_001", "可靠性不丢消息"));
        ThreadUtil.sleep(WAIT_CONSUME_MS);
    }
}
