package lan.chaos.finance.common.model;

import lan.chaos.finance.common.constant.OrderStatus;
import lombok.Data;

/**
 * 交易订单（支付引擎的编排对象）。
 *
 * <p>WHY：{@code bizNo} 是「唯一业务流水号」，幂等控制的锚点 —— 同一笔业务请求
 * 无论被重发多少次，靠它识别为同一笔（对话：金融极度厌恶重复扣款）。
 * {@code version} 为乐观锁版本号，配合状态机前置校验挡并发重复扣减。
 */
@Data
public class TradeOrder {
    private String id;
    /** 唯一业务流水号（幂等键） */
    private String bizNo;
    private OrderStatus status;
    private Money amount;
    private String payerId;
    private String payeeId;
    /** 乐观锁版本号 */
    private long version;
}
