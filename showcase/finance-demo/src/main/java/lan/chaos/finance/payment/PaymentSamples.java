package lan.chaos.finance.payment;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.constant.OrderStatus;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.TradeOrder;

/** 支付场景样例数据工厂（AGENTS.md 第三章：调用方无需自备输入）。 */
public final class PaymentSamples {

    private PaymentSamples() {
    }

    public static TradeOrder newOrder(String bizNo, Money amount) {
        TradeOrder order = new TradeOrder();
        order.setId("ORD-" + bizNo);
        order.setBizNo(bizNo);
        order.setStatus(OrderStatus.INIT);
        order.setAmount(amount);
        order.setPayerId("alice");
        order.setPayeeId("bob");
        order.setVersion(0);
        return order;
    }

    public static Money sampleAmount() {
        return Money.of("100.00", Currency.CNY);
    }

    public static String sampleBizNo() {
        return "BIZ-PAY-001";
    }
}
