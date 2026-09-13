package lan.chaos.crypto.common.util;

import java.util.Base64;

/**
 * 字节 ↔ Base64 工具。
 *
 * <p>WHY：密文/签名是二进制，落库或走 HTTP 时需要「可打印字符」表示。
 * 生产里 Base64 比 Hex 更常用——同样一段数据，Base64 只膨胀 4/3，Hex 膨胀 2 倍。
 *
 * <p>注意：Base64 <b>不是加密</b>，只是编码，任何人都能还原；它解决的是「二进制怎么放进文本协议」。
 */
public final class Base64Util {

    private Base64Util() {
    }

    public static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /** URL 安全变体：把 {@code +/} 换成 {@code -_}，用于 JWT、URL 参数等场景。 */
    public static String encodeUrl(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public static byte[] decodeUrl(String base64) {
        return Base64.getUrlDecoder().decode(base64);
    }
}
