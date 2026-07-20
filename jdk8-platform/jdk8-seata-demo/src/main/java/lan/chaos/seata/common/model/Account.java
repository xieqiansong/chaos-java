package lan.chaos.seata.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 账户模型。
 *
 * @author chaos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private Long id;
    /** 用户标识 */
    private String userId;
    /** 可用余额 */
    private double balance;
    /** TCC 冻结金额 */
    private double frozen;
}
