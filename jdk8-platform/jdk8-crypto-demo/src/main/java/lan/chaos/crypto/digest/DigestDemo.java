package lan.chaos.crypto.digest;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.CryptoProviders;
import lan.chaos.crypto.common.util.HexUtil;

import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ★★★ 高频：摘要 / 哈希 —— 把任意长度数据压成固定长度指纹，单向不可逆。
 *
 * <p>痛点：比对大文件是否一致、防篡改、给密码「留个痕迹但不留原文」，都靠摘要。
 * 但「用哪个摘要」差异巨大，本类把主流摘要一次性列出并对比输出长度。
 *
 * <table border="1">
 *   <tr><th>算法</th><th>输出</th><th>状态</th><th>说明</th></tr>
 *   <tr><td>MD5</td><td>128 位</td><td>禁用</td><td>碰撞秒破，仅可做非安全校验和</td></tr>
 *   <tr><td>SHA-1</td><td>160 位</td><td>禁用</td><td>SHAttered 已实际构造碰撞</td></tr>
 *   <tr><td>SHA-256 / 384 / 512</td><td>256/384/512 位</td><td>推荐</td><td>当前安全基线（SHA-2 家族）</td></tr>
 *   <tr><td>SHA3-256</td><td>256 位</td><td>推荐</td><td>Keccak 海绵结构，与 SHA-2 无共同弱点；JDK8 需 BC</td></tr>
 *   <tr><td>SM3</td><td>256 位</td><td>推荐</td><td>国密哈希（见 {@code sm/SmCryptoDemo}）</td></tr>
 * </table>
 *
 * <p>生产坑：
 * <ul>
 *   <li><b>密码绝不能明文存储，也不能只存 SHA-256</b>——无盐、无慢化，
 *       彩虹表和 GPU 暴力破解可秒破。密码必须加盐 + 慢哈希（Argon2 / bcrypt / scrypt），
 *       见 {@code kdf/KdfDemo}。</li>
 *   <li>摘要<b>不能当 MAC 用</b>：谁都能算出 {@code SHA256(消息)}，无法证明来源；
 *       要「带密钥的摘要」用 HMAC（见 {@code MacDemo}）。</li>
 *   <li>能「碰撞」（两段不同数据同摘要）的摘要一律不能再用于签名与证书：MD5 已破，SHA-1 已破。</li>
 *   <li>比对时字符串编码 / 换行必须统一，否则同一内容摘要也不同。</li>
 * </ul>
 */
public class DigestDemo {

    /** SHA-3 在 JDK8 原生没有，必须走 BouncyCastle。 */
    public static final String SHA3_256 = "SHA3-256";

    static {
        CryptoProviders.ensureBouncyCastle();
    }

    private DigestDemo() {
    }

    /** 通用摘要：默认提供者优先，找不到再落到 BC（SHA3 等）。 */
    public static byte[] digest(String algorithm, byte[] data) throws Exception {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(algorithm);
        } catch (java.security.NoSuchAlgorithmException e) {
            messageDigest = MessageDigest.getInstance(algorithm, CryptoProviders.BC);
        }
        return messageDigest.digest(data);
    }

    public static String digestHex(String algorithm, byte[] data) throws Exception {
        return HexUtil.toHex(digest(algorithm, data));
    }

    // ------------------------------------------------------------------ 常用便捷方法

    /** MD5 十六进制摘要（32 字符，仅供非安全校验和）。 */
    public static String md5Hex(byte[] data) throws Exception {
        return digestHex("MD5", data);
    }

    /** SHA-1 十六进制摘要（40 字符，已不安全）。 */
    public static String sha1Hex(byte[] data) throws Exception {
        return digestHex("SHA-1", data);
    }

    /** SHA-256 十六进制摘要（64 字符，当前基线）。 */
    public static String sha256Hex(byte[] data) throws Exception {
        return digestHex("SHA-256", data);
    }

    /** SHA-384 十六进制摘要（96 字符）。 */
    public static String sha384Hex(byte[] data) throws Exception {
        return digestHex("SHA-384", data);
    }

    /** SHA-512 十六进制摘要（128 字符）。 */
    public static String sha512Hex(byte[] data) throws Exception {
        return digestHex("SHA-512", data);
    }

    /** SHA3-256 十六进制摘要（64 字符，需 BouncyCastle）。 */
    public static String sha3_256Hex(byte[] data) throws Exception {
        return digestHex(SHA3_256, data);
    }

    /** 渐进式摘要：适合大文件分块读取，避免一次性 load 进内存。 */
    public static String sha256HexOfChunks(byte[] data, int chunkSize) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        for (int offset = 0; offset < data.length; offset += chunkSize) {
            int len = Math.min(chunkSize, data.length - offset);
            messageDigest.update(data, offset, len);
        }
        return HexUtil.toHex(messageDigest.digest());
    }

    public static void main(String[] args) throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] data = sample.toBytes();

        Map<String, String> digests = new LinkedHashMap<String, String>();
        digests.put("MD5(禁用)", md5Hex(data));
        digests.put("SHA-1(禁用)", sha1Hex(data));
        digests.put("SHA-256", sha256Hex(data));
        digests.put("SHA-384", sha384Hex(data));
        digests.put("SHA-512", sha512Hex(data));
        digests.put("SHA3-256", sha3_256Hex(data));

        System.out.println("[摘要对比] 原文=" + sample.plaintext());
        for (Map.Entry<String, String> entry : digests.entrySet()) {
            System.out.printf("  %-14s %d 字符  %s%n",
                    entry.getKey(), entry.getValue().length(), entry.getValue());
        }

        System.out.println("\n[雪崩效应] 原文改 1 个字符，SHA-256 摘要变化：");
        System.out.println("  原 文=" + sha256Hex(data));
        System.out.println("  改后=" + sha256Hex((sample.plaintext() + "x").getBytes("UTF-8")));

        System.out.println("\n[分块摘要] 同内容分块 update 结果一致="
                + sha256Hex(data).equals(sha256HexOfChunks(data, 7)) + "（大文件不必一次读入内存）");
    }
}
