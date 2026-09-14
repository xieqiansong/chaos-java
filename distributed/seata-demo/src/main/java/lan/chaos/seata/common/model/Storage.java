package lan.chaos.seata.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存模型。
 *
 * @author chaos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Storage {
    private Long id;
    /** 商品标识 */
    private String productId;
    /** 总库存 */
    private int total;
    /** TCC 冻结库存 */
    private int frozen;
}
