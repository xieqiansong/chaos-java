package lan.chaos.finance.payment;

import lan.chaos.finance.common.model.TradeOrder;

/**
 * 支付结果：区分「本次真实扣款」与「重复请求命中幂等」。
 *
 * <p>WHY：幂等的返回必须"结果一致"——重复请求也返回同一个订单、同一个终态，
 * 只是不会再次扣款。用 {@code duplicated} 让调用方/测试可观察"到底扣了几次"。
 */
public class PayResult {
    private final TradeOrder order;
    private final boolean duplicated;

    public PayResult(TradeOrder order, boolean duplicated) {
        this.order = order;
        this.duplicated = duplicated;
    }

    public TradeOrder getOrder() {
        return order;
    }

    /** true = 本次为重复请求，未再扣款；false = 本次完成真实扣款。 */
    public boolean isDuplicated() {
        return duplicated;
    }

    @Override
    public String toString() {
        return "PayResult{order=" + order.getBizNo()
                + ", status=" + order.getStatus()
                + ", duplicated=" + duplicated + '}';
    }
}
