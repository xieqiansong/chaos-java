package lan.chaos.finance.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 脱敏 + 不可篡改审计日志断言测试。 */
class AuditTest {

    @Test
    void 卡号手机号身份证脱敏() {
        assertThat(MaskUtils.maskCard("6222021234567890")).isEqualTo("************7890");
        assertThat(MaskUtils.maskPhone("13800138000")).isEqualTo("*******8000");
        assertThat(MaskUtils.maskIdCard("110101199001011234")).isEqualTo("**************1234");
        assertThat(MaskUtils.maskName("张三")).isEqualTo("张*");
    }

    @Test
    void 过短输入全遮避免泄露() {
        assertThat(MaskUtils.maskCard("123")).isEqualTo("***");
        assertThat(MaskUtils.maskCard(null)).isEqualTo("***");
    }

    @Test
    void 审计日志appendOnly且不含完整敏感信息() {
        AuditLogger logger = new AuditLogger();
        logger.record("teller01", "PAY", "BIZ-PAY-001", "6222021234567890");
        logger.record("checker02", "REVIEW", "BIZ-PAY-001", "6222021234567890");

        assertThat(logger.size()).isEqualTo(2);
        AuditEntry first = logger.snapshot().get(0);
        assertThat(first.getOperator()).isEqualTo("teller01");
        assertThat(first.getOccurredAt()).isNotNull();
        assertThat(first.getMaskedDetail()).doesNotContain("6222021234567890");
        assertThat(first.getMaskedDetail()).endsWith("7890");
    }

    @Test
    void 快照不可修改() {
        AuditLogger logger = new AuditLogger();
        logger.record("op", "ACT", "T", "6222021234567890");
        assertThatThrownBy(() -> logger.snapshot().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(logger.size()).isEqualTo(1); // 内部列表未被污染
    }
}
