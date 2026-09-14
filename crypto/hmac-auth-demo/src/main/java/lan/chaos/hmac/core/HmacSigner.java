package lan.chaos.hmac.core;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * HMAC-SHA256 签名生成与校验。
 *
 * 签名串规范（按行拼接，防止字段歧义）：
 * <pre>
 *   method\npath\ntimestamp\nnonce\nbodyDigest
 * </pre>
 * 其中 {@code bodyDigest} 为 SHA-256(body) 的 hex 小写，用于保证请求体完整性。
 *
 * 验签使用常量时间比较（{@link MessageDigest#isEqual}），防止时序侧信道。
 */
public final class HmacSigner {

    private static final SecureRandom RANDOM = new SecureRandom();

    private HmacSigner() {
    }

    /** 构造规范化签名串。 */
    public static String buildSignContent(String method, String path, long timestamp,
                                          String nonce, String body) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(method).append('\n')
                .append(path).append('\n')
                .append(timestamp).append('\n')
                .append(nonce).append('\n')
                .append(sha256Hex(body));
        return sb.toString();
    }

    /** 生成 HMAC-SHA256 签名，Base64 输出。 */
    public static String sign(String secret, String method, String path, long timestamp,
                              String nonce, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(
                    buildSignContent(method, path, timestamp, nonce, body)
                            .getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("hmac sign failed", e);
        }
    }

    /** 验签：常量时间比较，防时序攻击。 */
    public static boolean verify(String secret, String method, String path, long timestamp,
                                 String nonce, String body, String sign) {
        if (sign == null || sign.isEmpty()) {
            return false;
        }
        String expected = sign(secret, method, path, timestamp, nonce, body);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sign.getBytes(StandardCharsets.UTF_8));
    }

    /** SHA-256 hex 小写摘要。 */
    public static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 生成随机 nonce（16 字节随机数 hex，32 字符）。 */
    public static String newNonce() {
        byte[] buf = new byte[16];
        RANDOM.nextBytes(buf);
        return toHex(buf);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
