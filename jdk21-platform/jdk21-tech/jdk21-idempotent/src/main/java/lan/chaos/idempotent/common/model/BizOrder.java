package lan.chaos.idempotent.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用业务单据（泛化实体，不绑定任何具体业务语义）。
 * 仅用于演示「写动作只应执行一次」——幂等防护包裹的就是对它的落库。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BizOrder {
    /** 业务单号，天然唯一键，也是 STATE 场景的去重键 */
    private String bizNo;
    /** 动作：CREATE / CONFIRM / CANCEL 等状态迁移 */
    private String action;
    /** 处理后到达的终态 */
    private String state;
}
