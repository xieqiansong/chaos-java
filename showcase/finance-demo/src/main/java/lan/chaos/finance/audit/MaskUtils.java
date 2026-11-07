package lan.chaos.finance.audit;

/**
 * 敏感数据脱敏工具（P4，对应对话「敏感数据保护」）。
 *
 * <p>WHY：卡号、身份证号、手机号在日志、报文、数据库中都要脱敏或加密，
 * <b>日志里打印完整敏感信息是严重违规</b>。统一走这里，杜绝各处手写 substring 漏遮。
 *
 * <p>策略：只保留尾号用于人工核对，中间位用 {@code *} 遮蔽。
 */
public final class MaskUtils {

    private MaskUtils() {
    }

    /** 卡号：保留后 4 位，如 {@code 6222021234567890 → ************7890}。 */
    public static String maskCard(String cardNo) {
        return mask(cardNo, 4);
    }

    /** 手机号：保留后 4 位，如 {@code 13800138000 → *******8000}。 */
    public static String maskPhone(String phone) {
        return mask(phone, 4);
    }

    /** 身份证：保留后 4 位。 */
    public static String maskIdCard(String idCard) {
        return mask(idCard, 4);
    }

    /** 姓名：保留姓氏，其余打码。 */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "***";
        }
        return name.charAt(0) + repeat('*', Math.max(1, name.length() - 1));
    }

    private static String mask(String raw, int keepTail) {
        if (raw == null) {
            return "***";
        }
        if (raw.length() <= keepTail) {
            return repeat('*', raw.length()); // 太短则全遮，避免泄露
        }
        return repeat('*', raw.length() - keepTail) + raw.substring(raw.length() - keepTail);
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
