package lan.chaos.crypto.kdf;

import lan.chaos.crypto.common.util.HexUtil;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;

/**
 * ★★★ 推荐：KDF（密钥派生函数）—— 把「口令 / 共享秘密」加工成可用的密钥。
 *
 * <p>痛点：用户口令是低熵的（几个字、几万个常见组合），
 * <b>绝不能直接当密钥用</b>，更不能「SHA-256 一下」就当密码哈希存库——
 * 攻击者拿彩虹表 / GPU 每秒能试几十亿次。KDF 的职责是：加盐（防彩虹表）+ 慢化（抬高每次尝试成本）。
 *
 * <p>本类两种最常用 KDF：
 * <ul>
 *   <li><b>PBKDF2</b>（{@code PBKDF2WithHmacSHA256}）：JDK 原生支持，
 *       靠「迭代次数」把单次计算成本放大 10 万倍以上；优点是标准、无依赖，缺点是内存占用小、GPU 仍可加速。</li>
 *   <li><b>HKDF</b>（BouncyCastle 轻量 API）：不是给口令用的，而是把
 *       <b>已经足够随机的共享秘密</b>（如 ECDH 结果）派生成多把用途隔离的子密钥。分「提取 + 扩展」两阶段。</li>
 * </ul>
 *
 * <p>生产坑：
 * <ul>
 *   <li><b>盐必须随机、每用户独立</b>（16 字节以上），并和哈希一起存库；盐不是秘密，但不能复用。</li>
 *   <li>PBKDF2 迭代次数建议 ≥ 10 万（OWASP 2023 对 SHA-256 的建议是 60 万），按服务器预算调优并可逐年提高。</li>
 *   <li>密码<b>存储</b>场景，如今首选 Argon2id（内存硬，抗 GPU/ASIC），其次 bcrypt / scrypt；
 *       PBKDF2 是「合规 / FIPS 环境」的稳妥备选。</li>
 *   <li>PBKDF2 输出长度 ≤ 底层哈希长度最自然（SHA-256 → 32 字节），要更长会退化多次迭代。</li>
 * </ul>
 */
public class KdfDemo {

    /** JDK8 原生支持 PBKDF2 + HMAC-SHA256。 */
    public static final String PBKDF2_WITH_HMAC_SHA256 = "PBKDF2WithHmacSHA256";

    private static final SecureRandom RNG = new SecureRandom();

    private KdfDemo() {
    }

    /** 生成随机盐（建议 16 字节以上）。 */
    public static byte[] randomSalt(int len) {
        byte[] salt = new byte[len];
        RNG.nextBytes(salt);
        return salt;
    }

    /**
     * PBKDF2 派生密钥。
     *
     * @param password   口令
     * @param salt       随机盐（与结果一起存库）
     * @param iterations 迭代次数（越大越慢、越难暴力破解）
     * @param keyLength  输出位长（如 256）
     */
    public static byte[] pbkdf2(String password, byte[] salt, int iterations, int keyLength) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        return SecretKeyFactory.getInstance(PBKDF2_WITH_HMAC_SHA256).generateSecret(spec).getEncoded();
    }

    public static String pbkdf2Hex(String password, byte[] salt, int iterations, int keyLength) throws Exception {
        return HexUtil.toHex(pbkdf2(password, salt, iterations, keyLength));
    }

    /**
     * HKDF（提取-扩展）：从共享秘密派生指定长度的子密钥。
     *
     * @param inputKeyMaterial 输入密钥材料（如 ECDH 共享秘密）
     * @param salt             盐（可为 null）
     * @param info             上下文标签，用于「同一主密钥派生不同用途的子密钥」
     * @param lengthBits       输出位长
     */
    public static byte[] hkdf(byte[] inputKeyMaterial, byte[] salt, byte[] info, int lengthBits) {
        HKDFBytesGenerator generator = new HKDFBytesGenerator(new SHA256Digest());
        generator.init(new HKDFParameters(inputKeyMaterial, salt, info));
        byte[] out = new byte[lengthBits / 8];
        generator.generateBytes(out, 0, out.length);
        return out;
    }

    public static void main(String[] args) throws Exception {
        String password = "P@ssw0rd-示例";
        byte[] salt = randomSalt(16);

        System.out.println("[PBKDF2] 口令=" + password + "  盐=" + HexUtil.toHex(salt));

        long start = System.nanoTime();
        String weak = pbkdf2Hex(password, salt, 1_000, 256);
        long weakCost = System.nanoTime() - start;
        start = System.nanoTime();
        String strong = pbkdf2Hex(password, salt, 200_000, 256);
        long strongCost = System.nanoTime() - start;

        System.out.println("  1,000 次迭代  → " + weak.substring(0, 32) + "...  耗时 " + (weakCost / 1_000_000) + " ms");
        System.out.println("  200,000 次迭代→ " + strong.substring(0, 32) + "...  耗时 " + (strongCost / 1_000_000) + " ms");
        System.out.println("  同一口令 + 换盐 → " + pbkdf2Hex(password, randomSalt(16), 200_000, 256).substring(0, 32)
                + "...  ← 盐不同结果完全不同，彩虹表失效");
        System.out.println("  迭代 200 倍 → 攻击者暴力破解成本也高 200 倍");

        // HKDF：把 ECDH 共享秘密派生成「加密用」与「MAC 用」两把子密钥
        byte[] sharedSecret = randomSalt(32);
        byte[] encKey = hkdf(sharedSecret, salt, "aes-gcm-key".getBytes("UTF-8"), 256);
        byte[] macKey = hkdf(sharedSecret, salt, "hmac-key".getBytes("UTF-8"), 256);
        System.out.println("\n[HKDF] 共享秘密=" + HexUtil.toHex(sharedSecret).substring(0, 32) + "...");
        System.out.println("  派生加密密钥=" + HexUtil.toHex(encKey).substring(0, 32) + "...");
        System.out.println("  派生 MAC密钥 =" + HexUtil.toHex(macKey).substring(0, 32) + "...");
        System.out.println("  同一主密钥 + 不同 info → 用途隔离，互不泄露");
        System.out.println("  info 相同的派生可复现="
                + HexUtil.toHex(encKey).equals(HexUtil.toHex(hkdf(sharedSecret, salt, "aes-gcm-key".getBytes("UTF-8"), 256))));
    }
}
