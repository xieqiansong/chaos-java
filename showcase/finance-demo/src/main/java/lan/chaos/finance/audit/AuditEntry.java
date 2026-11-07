package lan.chaos.finance.audit;

import lombok.Data;

import java.time.LocalDateTime;

/** 审计条目：谁、何时、做了什么、作用对象、脱敏后的明细。 */
@Data
public class AuditEntry {
    private long id;
    private String operator;
    private String action;
    private String target;
    /** 注意：明细必须已脱敏，禁止写入完整卡号/证件号/手机号。 */
    private String maskedDetail;
    private LocalDateTime occurredAt;
}
