package lan.chaos.crypto.aes;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.HexUtil;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * ★★★ 高频：AES 对称加密 —— 用同一把密钥加解密，速度快，适合加密大量业务数据。
 *
 * <p>痛点：明文落库/传输会被窃取，需要可逆加密。AES 是对称加密金标准。
 *
 * <p>两种模式（生产最常区分）：
 * <ul>
 *   <li><b>CBC</b>（密码分组链接）：需要随机 IV，仅保证机密性，不防篡改。常用
 *       {@code AES/CBC/PKCS5Padding}，IV 必须随机且随密文一起传给解密方。</li>
 *   <li><b>GCM</b>（ Galois/Counter Mode）：认证加密（AEAD），<b>同时保证机密性 + 完整性</b>，
 *       自带 MAC 校验，可附加 AAD（额外认证数据）。推荐新项目默认 GCM。</li>
 * </ul>
 *
 * <p>关键 API：{@code Cipher.getInstance("AES/CBC/PKCS5Padding" | "AES/GCM/NoPadding")}、
 * {@code KeyGenerator("AES")}、{@code IvParameterSpec}/{@code GCMParameterSpec}。
 *
 * <p>生产坑：
 * <ul>
 *   <li>密钥不能硬编码，应放 KMS/配置中心；IV 必须随机且<b>每次加密不同</b>。</li>
 *   <li>CBC 无完整性校验，被篡改不会报错，需搭配 HMAC 或改用 GCM。</li>
 *   <li>GCM 的 IV 切勿复用，复用会直接泄露明文与认证密钥。</li>
 *   <li>JDK8 默认 AES 限 128 位，256 位需装 JCE 无限强度策略文件。</li>
 * </ul>
 */
public class AesDemo {

    private static final SecureRandom RNG = new SecureRandom();

    /** 生成 AES 密钥（bits=128/192/256）。 */
    public static byte[] genKey(int bits) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(bits);
        return kg.generateKey().getEncoded();
    }

    /** 生成随机 IV（CBC 用 16 字节）。 */
    public static byte[] genIv(int len) {
        byte[] iv = new byte[len];
        RNG.nextBytes(iv);
        return iv;
    }

    // ===== CBC 模式 =====
    public static byte[] encryptCbc(byte[] key, byte[] iv, byte[] plaintext) throws Exception {
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return c.doFinal(plaintext);
    }

    public static byte[] decryptCbc(byte[] key, byte[] iv, byte[] ciphertext) throws Exception {
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return c.doFinal(ciphertext);
    }

    // ===== GCM 模式（认证加密）=====
    public static byte[] encryptGcm(byte[] key, byte[] iv, byte[] plaintext) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return c.doFinal(plaintext); // Java 默认把 128 位 tag 附在密文末尾
    }

    public static byte[] decryptGcm(byte[] key, byte[] iv, byte[] ciphertext) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return c.doFinal(ciphertext);
    }

    /** 控制台演示：CBC + GCM 往返并打印输入→输出。 */
    public static void main(String[] args) throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();
        byte[] key = genKey(128);

        byte[] iv = genIv(16);
        byte[] ct = encryptCbc(key, iv, s.toBytes());
        byte[] pt = decryptCbc(key, iv, ct);
        System.out.printf("[AES/CBC] 密文=%s%n  解密后=%s%n", HexUtil.toHex(ct), new String(pt));

        byte[] iv12 = genIv(12);
        byte[] ct2 = encryptGcm(key, iv12, s.toBytes());
        byte[] pt2 = decryptGcm(key, iv12, ct2);
        System.out.printf("[AES/GCM] 密文(含tag)=%s%n  解密后=%s%n", HexUtil.toHex(ct2), new String(pt2));
    }
}
