package lan.chaos.serialization.common.util;

import java.nio.charset.StandardCharsets;

/** 序列化体积对比用的小工具：统一用 UTF-8 字节长度衡量文本序列化结果。 */
public final class ByteSizeUtil {
    private ByteSizeUtil() {}

    public static int utf8Bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }

    public static int bytes(byte[] data) {
        return data == null ? 0 : data.length;
    }
}
