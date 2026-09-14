package lan.chaos.mybatisplus.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加解密工具（演示用）。
 * 注意：这里用硬编码密钥 + AES/ECB 仅为跑通 demo；生产应使用 AES/GCM、密钥放 KMS / 配置中心，
 * 并配合独立的数据密钥管理，严禁把密钥写在代码里。
 */
public final class AesUtil {
    private static final String KEY = "demo-key-1234567"; // 16 字节
    private static final String ALGO = "AES/ECB/PKCS5Padding";

    private AesUtil() {
    }

    public static String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            Cipher c = Cipher.getInstance(ALGO);
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES"));
            return Base64.getEncoder().encodeToString(c.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("AES encrypt failed", e);
        }
    }

    public static String decrypt(String cipher) {
        if (cipher == null) {
            return null;
        }
        try {
            Cipher c = Cipher.getInstance(ALGO);
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES"));
            return new String(c.doFinal(Base64.getDecoder().decode(cipher)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES decrypt failed", e);
        }
    }
}
