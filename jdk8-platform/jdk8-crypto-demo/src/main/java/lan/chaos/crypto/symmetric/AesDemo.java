package lan.chaos.crypto.symmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.Base64Util;
import lan.chaos.crypto.common.util.HexUtil;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ★★★ 高频：AES 对称加密 —— 加解密同一把密钥，速度快，是业务数据的加密标准。
 *
 * <p>痛点：明文落库 / 传输会被窃取，需要可逆加密。AES 是分组密码（分组 128 位），
 * 不同<b>工作模式</b>决定了「是否需要 IV、能否防篡改、能否并行」，选错模式等于没加密。
 *
 * <p>本类覆盖主流的 6 种模式，按安全性从高到低：
 * <ul>
 *   <li><b>GCM</b>（{@code AES/GCM/NoPadding}）：认证加密（AEAD），机密性 + 完整性一次到位，
 *       自带 MAC，可附加 AAD（额外认证数据）。<b>新项目默认选它</b>。</li>
 *   <li><b>CBC</b>（{@code AES/CBC/PKCS5Padding}）：需随机 IV，只保机密性，
 *       被篡改<b>不会报错</b>，必须另配 HMAC。</li>
 *   <li><b>CTR / CFB / OFB</b>：流式模式，无需填充，同样不防篡改（要配 MAC）。</li>
 *   <li><b>ECB</b>（{@code AES/ECB/PKCS5Padding}）：无 IV、逐块独立加密，
 *       相同明文块 → 相同密文，泄露数据轮廓，<b>禁止用于业务</b>（本类仅作反面对照）。</li>
 * </ul>
 *
 * <p>关键 API：{@code Cipher.getInstance(转换串)}、{@code KeyGenerator("AES")}、
 * {@code IvParameterSpec}（CBC/CTR/CFB/OFB）、{@code GCMParameterSpec}（GCM）。
 *
 * <p>生产坑：
 * <ul>
 *   <li>密钥不能硬编码，放 KMS / 配置中心；IV / nonce 必须随机且<b>每次加密都不同</b>。</li>
 *   <li><b>GCM 的 nonce 绝不能复用</b>——同一密钥下复用 nonce 会泄露明文并伪造认证标签。</li>
 *   <li>CBC 无完整性，被篡改只是解出乱码；必须 Encrypt-then-MAC 或直接换 GCM。</li>
 *   <li>JDK8u161 起默认已支持 AES-256，无需再手动替换 JCE 无限强度策略文件。</li>
 * </ul>
 */
public class AesDemo {

    /** 算法族名。 */
    private static final String ALG = "AES";
    /** GCM 认证标签长度（位）。 */
    private static final int GCM_TAG_BITS = 128;
    /** GCM 推荐的 96 位 nonce。 */
    private static final int GCM_NONCE_LEN = 12;
    /** CBC / CTR / CFB / OFB 的 IV 长度（AES 分组 128 位）。 */
    private static final int IV_LEN = 16;

    /** ECB：无 IV，相同明文块产生相同密文，仅作反面对照。 */
    public static final String ECB = "AES/ECB/PKCS5Padding";
    /** CBC：需随机 IV，仅机密性。 */
    public static final String CBC = "AES/CBC/PKCS5Padding";
    /** CTR：流式，无需填充，需唯一 nonce。 */
    public static final String CTR = "AES/CTR/NoPadding";
    /** CFB：流式，老协议兼容。 */
    public static final String CFB = "AES/CFB/NoPadding";
    /** OFB：流式，老协议兼容。 */
    public static final String OFB = "AES/OFB/NoPadding";
    /** GCM：认证加密，推荐默认。 */
    public static final String GCM = "AES/GCM/NoPadding";

    private static final SecureRandom RNG = new SecureRandom();

    private AesDemo() {
    }

    // ------------------------------------------------------------------ 密钥 / IV

    /** 生成 AES 密钥（bits = 128 / 192 / 256）。 */
    public static byte[] genKey(int bits) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(ALG);
        kg.init(bits);
        return kg.generateKey().getEncoded();
    }

    /** 生成长度为 len 的随机字节。 */
    public static byte[] randomBytes(int len) {
        byte[] bytes = new byte[len];
        RNG.nextBytes(bytes);
        return bytes;
    }

    /** CBC / CTR / CFB / OFB 用的 16 字节随机 IV。 */
    public static byte[] genIv() {
        return randomBytes(IV_LEN);
    }

    /** GCM 用的 12 字节随机 nonce。 */
    public static byte[] genNonce() {
        return randomBytes(GCM_NONCE_LEN);
    }

    // ------------------------------------------------------------------ 通用加解密

    /**
     * 通用加密：{@code iv == null} 视为 ECB（无 IV）；GCM 自动走 {@link GCMParameterSpec}。
     */
    public static byte[] encrypt(String transformation, byte[] key, byte[] iv, byte[] plaintext) throws Exception {
        return newCipher(transformation, Cipher.ENCRYPT_MODE, key, iv).doFinal(plaintext);
    }

    /** 通用解密，与 {@link #encrypt} 对称。 */
    public static byte[] decrypt(String transformation, byte[] key, byte[] iv, byte[] ciphertext) throws Exception {
        return newCipher(transformation, Cipher.DECRYPT_MODE, key, iv).doFinal(ciphertext);
    }

    private static Cipher newCipher(String transformation, int mode, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation);
        SecretKeySpec keySpec = new SecretKeySpec(key, ALG);
        if (iv == null) {
            cipher.init(mode, keySpec);
        } else if (transformation.contains("/GCM/")) {
            cipher.init(mode, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
        } else {
            cipher.init(mode, keySpec, new IvParameterSpec(iv));
        }
        return cipher;
    }

    // ------------------------------------------------------------------ 常用便捷方法

    public static byte[] encryptCbc(byte[] key, byte[] iv, byte[] plaintext) throws Exception {
        return encrypt(CBC, key, iv, plaintext);
    }

    public static byte[] decryptCbc(byte[] key, byte[] iv, byte[] ciphertext) throws Exception {
        return decrypt(CBC, key, iv, ciphertext);
    }

    public static byte[] encryptGcm(byte[] key, byte[] nonce, byte[] plaintext) throws Exception {
        return encrypt(GCM, key, nonce, plaintext);
    }

    public static byte[] decryptGcm(byte[] key, byte[] nonce, byte[] ciphertext) throws Exception {
        return decrypt(GCM, key, nonce, ciphertext);
    }

    /**
     * GCM + AAD（附加认证数据）：AAD 参与认证但不加密，
     * 适合把「协议头 / 业务主键 / 版本号」等必须一致又不能暴露的元数据绑进认证。
     */
    public static byte[] encryptGcmWithAad(byte[] key, byte[] nonce, byte[] aad, byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(GCM);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALG), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(plaintext);
    }

    public static byte[] decryptGcmWithAad(byte[] key, byte[] nonce, byte[] aad, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance(GCM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ALG), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(ciphertext);
    }

    // ------------------------------------------------------------------ 演示

    public static void main(String[] args) throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] key128 = genKey(128);
        byte[] key256 = genKey(256);
        System.out.println("[AES] 密钥 128 位=" + HexUtil.toHex(key128));
        System.out.println("[AES] 密钥 256 位=" + HexUtil.toHex(key256) + "（JDK8u161+ 默认支持）");

        // 各模式往返：同一段明文，观察密文长度与结构差异
        Map<String, byte[]> ivs = new LinkedHashMap<String, byte[]>();
        ivs.put(ECB, null);
        ivs.put(CBC, genIv());
        ivs.put(CTR, genIv());
        ivs.put(CFB, genIv());
        ivs.put(OFB, genIv());
        ivs.put(GCM, genNonce());

        System.out.println("\n[往返验证] 同一明文在各模式下的加解密：");
        for (Map.Entry<String, byte[]> entry : ivs.entrySet()) {
            String transformation = entry.getKey();
            byte[] iv = entry.getValue();
            byte[] ciphertext = encrypt(transformation, key128, iv, sample.toBytes());
            byte[] plaintext = decrypt(transformation, key128, iv, ciphertext);
            System.out.printf("  %-24s 密文 %3d 字节  Base64=%s%n  还原成功=%s%n",
                    transformation, ciphertext.length, Base64Util.encode(ciphertext),
                    sample.plaintext().equals(new String(plaintext)));
        }

        // ECB 反面对照：两块相同明文 → 两块相同密文
        byte[] repeated = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA".getBytes("UTF-8");
        byte[] ecbCipher = encrypt(ECB, key128, null, repeated);
        byte[] cbcCipher = encrypt(CBC, key128, genIv(), repeated);
        System.out.println("\n[ECB 反例] 32 字节重复明文：");
        System.out.println("  ECB 密文=" + HexUtil.toHex(ecbCipher) + " → 前后两块完全相同，泄露「数据重复」这一信息");
        System.out.println("  CBC 密文=" + HexUtil.toHex(cbcCipher) + " → 前一块影响后一块，重复被隐藏");

        // GCM 篡改检测
        byte[] nonce = genNonce();
        byte[] gcmCipher = encryptGcm(key128, nonce, sample.toBytes());
        try {
            gcmCipher[gcmCipher.length - 1] ^= 0x01;
            decryptGcm(key128, nonce, gcmCipher);
            System.out.println("\n[GCM 篡改] 未检测到（异常！）");
        } catch (Exception e) {
            System.out.println("\n[GCM 篡改] 解密果断失败：" + e.getClass().getSimpleName()
                    + " → 这正是认证加密的价值（CBC 此时只会解出乱码）");
        }
    }
}
