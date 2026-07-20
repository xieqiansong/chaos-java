package lan.chaos.seata.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单模型。
 *
 * @author chaos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    /** 下单用户 */
    private String userId;
    /** 商品 ID */
    private String productId;
    /** 订单金额 */
    private double amount;
    /** 订单状态：0-创建中 1-已完成  */
    private int status;
    /** 订单编号（唯一） */
    private String orderNo;
}
