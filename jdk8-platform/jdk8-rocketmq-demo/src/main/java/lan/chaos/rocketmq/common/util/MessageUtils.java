package lan.chaos.rocketmq.common.util;

import lan.chaos.rocketmq.common.model.Message;

/**
 * 消息信封的轻量封装/解析工具（不使用 JSON，避免序列化本身的开销污染"耗时"测量）。
 * <p>发送方用 {@link #pack(String)} 把正文打包成 {@code <时间戳毫秒>|<正文>}，
 * 消费方用 {@link #unpack(String)} 还原。</p>
 */
public final class MessageUtils {

    /** 时间戳与正文的分隔符（正文里即使含该字符也不影响解析，因为只按第一个分隔符切分） */
    private static final String SEP = "|";

    private MessageUtils() {
    }

    /** 用当前时间戳把业务正文打包成字符串 */
    public static String pack(String body) {
        return System.currentTimeMillis() + SEP + body;
    }

    /** 还原打包字符串为消息信封；解析失败/格式异常时时间戳记 0（消费侧据此跳过耗时计算） */
    public static Message unpack(String packed) {
        int idx = packed.indexOf(SEP);
        if (idx < 0) {
            return new Message(packed, 0L);
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(packed.substring(0, idx));
        } catch (NumberFormatException e) {
            timestamp = 0L;
        }
        return new Message(packed.substring(idx + SEP.length()), timestamp);
    }

    /** 计算"从发送时刻到现在的耗时（毫秒）"；时间戳为 0 时返回 -1 表示未知 */
    public static long costMillis(Message msg) {
        return msg.getTimestamp() == 0L ? -1 : System.currentTimeMillis() - msg.getTimestamp();
    }
}
