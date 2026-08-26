package lan.chaos.idempotent.common.util;

import lan.chaos.idempotent.common.model.BizOrder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 样例数据工厂：所有 demo 调用方无需自己准备输入。
 */
public final class SampleFactory {

    private static final AtomicLong SEQ = new AtomicLong(0);

    private SampleFactory() {
    }

    /** 生成一个全局唯一幂等号（模拟前端/网关下发） */
    public static String newRequestId() {
        return "req-" + UUID.randomUUID();
    }

    /** 生成一个 MQ messageId（模拟 broker 下发） */
    public static String newMessageId() {
        return "msg-" + UUID.randomUUID();
    }

    /** 生成一个业务单号 */
    public static String newBizNo() {
        return "BIZ-" + SEQ.incrementAndGet();
    }

    /** 造一条业务单据 */
    public static BizOrder newBizOrder(String action, String state) {
        return BizOrder.builder()
                .bizNo(newBizNo())
                .action(action)
                .state(state)
                .build();
    }
}
