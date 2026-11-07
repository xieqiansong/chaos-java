package lan.chaos.finance.common.model;

import lombok.Data;

/**
 * 分户账（客户视角看到的余额）。
 *
 * <p>WHY：客户看到的"我的余额"是分户账；机构层面还有按科目汇总的"总账"{@link Subject}。
 * 两者必须勾稽一致（分户账汇总 == 总账科目），这就是金融的"账户分层"。
 *
 * <p>{@code version} 是乐观锁版本号，真实库里用它挡"同一账户被并发透支/重复扣减"
 * （防御性思维①：被并发调用两次会怎样）。
 */
@Data
public class Account {
    private String id;
    private String ownerId;
    /** 所属总账科目编码 */
    private String subjectCode;
    private Money balance;
    /** 乐观锁版本号 */
    private long version;
}
