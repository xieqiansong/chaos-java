package lan.chaos.crypto.digest;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.CryptoProviders;
import lan.chaos.crypto.common.util.HexUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

/**
 * ★★★ 高频：MAC（消息认证码）—— 「带密钥的摘要」，验证完整性与来源。
 *
 * <p>痛点：裸摘要（SHA-256）谁都能算，无法证明消息<b>来自谁</b>；攻击者改了消息再重算摘要即可。
 * MAC 把一把共享密钥混入计算，只有持密钥的一方能算出正确值，
 * 于是同时证明了「消息没被改」+「消息来自持有密钥的对方」。
 *
 * <p>三种主流构造：
 * <ul>
 *   <li><b>HMAC-SHA256</b>（推荐）：基于哈希的 MAC，Java 原生支持，
 *       是接口签名 / JWT（HS256）/ 消息队列验签的事实标准。</li>
 *   <li><b>CMAC-AES</b>（推荐）：基于分组密码的 MAC，有 AES 硬件加速时比 HMAC 更快，
 *       适合嵌入式 / IoT，需 BouncyCastle。</li>
 *   <li><b>HMAC-SHA1</b>（遗留）：老协议 / 老 SDK 里常见，新系统不要再用。</li>
 * </ul>
 *
 * <p>生产坑：
 * <ul>
 *   <li><b>摘要 ≠ MAC</b>：{@code SHA256(msg)} 无法防伪造，务必换成 {@code HMAC(key, msg)}。</li>
 *   <li>比较 MAC 必须用<b>恒定时间比较</b>（{@code MessageDigest.isEqual}），
 *       用 {@code equals} 会因短路比较泄露时序信息。</li>
 *   <li>HMAC 的密钥要足够长（≥ 哈希输出长度，即 ≥32 字节），且每方独立保存。</li>
 *   <li>只做 MAC 不加密，消息本身仍是明文；要保密 + 认证一体请用 AEAD（AES-GCM）。</li>
 * </ul>
 */
public class MacDemo {

    /** 推荐：HMAC-SHA256。 */
    public static final String HMAC_SHA256 = "HmacSHA256";
    /** 遗留：HMAC-SHA1。 */
    public static final String HMAC_SHA1 = "HmacSHA1";
    /** 分组密码构造的 MAC（BouncyCastle）。 */
    public static final String CMAC_AES = "AESCMAC";

    static {
        CryptoProviders.ensureBouncyCastle();
    }

    private MacDemo() {
    }

    /** 通用 MAC 计算（HMAC 走原生，CMAC 自动落到 BC）。 */
    public static byte[] mac(String algorithm, byte[] key, byte[] data) throws Exception {
        Mac mac;
        try {
            mac = Mac.getInstance(algorithm);
        } catch (java.security.NoSuchAlgorithmException e) {
            mac = Mac.getInstance(algorithm, CryptoProviders.BC);
        }
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data);
    }

    public static String hmacSha256Hex(byte[] key, byte[] data) throws Exception {
        return HexUtil.toHex(mac(HMAC_SHA256, key, data));
    }

    public static String hmacSha1Hex(byte[] key, byte[] data) throws Exception {
        return HexUtil.toHex(mac(HMAC_SHA1, key, data));
    }

    /** CMAC-AES：key 必须是 AES 合法长度（16 / 24 / 32 字节）。 */
    public static String cmacAesHex(byte[] key, byte[] data) throws Exception {
        return HexUtil.toHex(mac(CMAC_AES, key, data));
    }

    /**
     * 恒定时间比较：无论在哪一位不同，耗时都一样，避免时序侧信道。
     * 注意别用 {@code Arrays.equals}——它发现不同就提前返回，比较耗时会泄露「前缀匹配了多少位」。
     */
    public static boolean constantTimeEquals(byte[] expected, byte[] actual) {
        return java.security.MessageDigest.isEqual(expected, actual);
    }

    public static void main(String[] args) throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] data = sample.toBytes();
        byte[] key = "shared-secret-key-0123456789abcdef".getBytes("UTF-8"); // 32 字节
        byte[] otherKey = "another-secret-key-9876543210fedcba".getBytes("UTF-8");

        System.out.println("[HMAC-SHA256] 摘要=" + hmacSha256Hex(key, data));
        System.out.println("  换个密钥=" + hmacSha256Hex(otherKey, data) + "  ← 密钥不同结果完全不同");
        System.out.println("  篡改消息=" + hmacSha256Hex(key, (sample.plaintext() + "x").getBytes("UTF-8")));
        System.out.println("[HMAC-SHA1(遗留)] " + hmacSha1Hex(key, data));

        byte[] aesKey = new byte[16];
        System.arraycopy(key, 0, aesKey, 0, 16);
        System.out.println("[CMAC-AES] " + cmacAesHex(aesKey, data));
        System.out.println("  篡改消息=" + cmacAesHex(aesKey, (sample.plaintext() + "x").getBytes("UTF-8")));

        byte[] expected = mac(HMAC_SHA256, key, data);
        System.out.println("\n[恒定时间比较] MessageDigest.isEqual 相等=" + constantTimeEquals(expected, expected)
                + "，不同密钥的 MAC 相等="
                + constantTimeEquals(expected, mac(HMAC_SHA256, otherKey, data))
                + "（不要用 Arrays.equals —— 已演示在下方）");
        System.out.println("  java.util.Arrays.equals 同样能比出结果，但它可能提前 return，泄露时序信息");
        System.out.println("  Arrays.equals(expected, expected)=" + Arrays.equals(expected, expected));
    }
}
