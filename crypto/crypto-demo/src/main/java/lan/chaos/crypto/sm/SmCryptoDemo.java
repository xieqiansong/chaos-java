package lan.chaos.crypto.sm;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.CryptoProviders;
import lan.chaos.crypto.common.util.HexUtil;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

/**
 * ★★★ 高频（合规场景）：国密算法套件 —— 中国商用密码标准，政务 / 金融 / 国企系统常强制要求。
 *
 * <p>三种核心国密（均由 BouncyCastle 提供，JDK8 原生没有）：
 * <ul>
 *   <li><b>SM4</b>：对称分组密码（128 位密钥、128 位分组），对标 AES。
 *       本类演示 CBC（机密性）与 GCM（推荐，机密性 + 完整性）。</li>
 *   <li><b>SM3</b>：哈希算法（256 位摘要），对标 SHA-256，用于完整性。</li>
 *   <li><b>SM2</b>：基于椭圆曲线（sm2p256v1）的非对称算法，对标 RSA / ECDSA，
 *       既能签名验签，也能公钥加密 / 私钥解密。</li>
 * </ul>
 *
 * <p>关键 API（必须指定 {@code "BC"} 提供者）：
 * <ul>
 *   <li>SM4：{@code Cipher.getInstance("SM4/CBC/PKCS5Padding", "BC")}</li>
 *   <li>SM3：{@code MessageDigest.getInstance("SM3", "BC")}</li>
 *   <li>SM2 签名：{@code Signature.getInstance("SM3withSM2", "BC")}</li>
 *   <li>SM2 加密：{@code Cipher.getInstance("SM2", "BC")}</li>
 * </ul>
 *
 * <p>生产坑：
 * <ul>
 *   <li>漏了 {@code ,"BC"} 会直接 {@code NoSuchAlgorithmException}——JDK8 原生没有 SM 系列。</li>
 *   <li>SM2 签名用 {@code SM3withSM2}（先对原文 SM3 再 SM2 签名），验签方算法与曲线必须一致。</li>
 *   <li>SM4 同样要求随机 IV / GCM nonce 且每次不同；合规场景推荐 SM4-GCM。</li>
 *   <li>互操作注意 SM2 加密结果的编排顺序（C1C3C2 vs C1C2C3），跨厂商对接要确认。</li>
 * </ul>
 */
public class SmCryptoDemo {

    private static final SecureRandom RNG = new SecureRandom();

    static {
        CryptoProviders.ensureBouncyCastle();
    }

    private SmCryptoDemo() {
    }

    // ================================================================== SM4 对称加密

    /** 生成 SM4 密钥（128 位）。 */
    public static byte[] genSm4Key() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("SM4", CryptoProviders.BC);
        generator.init(128);
        return generator.generateKey().getEncoded();
    }

    public static byte[] sm4EncryptCbc(byte[] key, byte[] iv, byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS5Padding", CryptoProviders.BC);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "SM4"), new IvParameterSpec(iv));
        return cipher.doFinal(plaintext);
    }

    public static byte[] sm4DecryptCbc(byte[] key, byte[] iv, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS5Padding", CryptoProviders.BC);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "SM4"), new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }

    /** SM4-GCM（认证加密，推荐）：nonce 12 字节，tag 128 位。 */
    public static byte[] sm4EncryptGcm(byte[] key, byte[] nonce, byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("SM4/GCM/NoPadding", CryptoProviders.BC);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "SM4"), new GCMParameterSpec(128, nonce));
        return cipher.doFinal(plaintext);
    }

    public static byte[] sm4DecryptGcm(byte[] key, byte[] nonce, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("SM4/GCM/NoPadding", CryptoProviders.BC);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "SM4"), new GCMParameterSpec(128, nonce));
        return cipher.doFinal(ciphertext);
    }

    // ================================================================== SM3 摘要

    /** SM3 十六进制摘要（64 字符，等价于 SHA-256 的输出长度）。 */
    public static String sm3Hex(byte[] data) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SM3", CryptoProviders.BC);
        return HexUtil.toHex(messageDigest.digest(data));
    }

    // ================================================================== SM2 非对称

    /** 生成 SM2 密钥对（曲线 sm2p256v1）。 */
    public static KeyPair genSm2KeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("SM2", CryptoProviders.BC);
        generator.initialize(new ECGenParameterSpec("sm2p256v1"), RNG);
        return generator.generateKeyPair();
    }

    /** SM2 私钥签名（SM3withSM2）。 */
    public static byte[] sm2Sign(PrivateKey privateKey, byte[] data) throws Exception {
        Signature signature = Signature.getInstance("SM3withSM2", CryptoProviders.BC);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    /** SM2 公钥验签。 */
    public static boolean sm2Verify(PublicKey publicKey, byte[] data, byte[] signatureBytes) throws Exception {
        Signature signature = Signature.getInstance("SM3withSM2", CryptoProviders.BC);
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }

    /** SM2 公钥加密。 */
    public static byte[] sm2Encrypt(PublicKey publicKey, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("SM2", CryptoProviders.BC);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /** SM2 私钥解密。 */
    public static byte[] sm2Decrypt(PrivateKey privateKey, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("SM2", CryptoProviders.BC);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(ciphertext);
    }

    // ================================================================== 演示

    public static void main(String[] args) throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] key = genSm4Key();

        byte[] iv = new byte[16];
        RNG.nextBytes(iv);
        byte[] cbcCipher = sm4EncryptCbc(key, iv, sample.toBytes());
        System.out.println("[SM4-CBC] 密钥=" + key.length * 8 + " 位 密文=" + HexUtil.toHex(cbcCipher));
        System.out.println("  解密后=" + new String(sm4DecryptCbc(key, iv, cbcCipher)));

        byte[] nonce = new byte[12];
        RNG.nextBytes(nonce);
        byte[] gcmCipher = sm4EncryptGcm(key, nonce, sample.toBytes());
        System.out.println("[SM4-GCM] 密文(含 tag)=" + HexUtil.toHex(gcmCipher));
        System.out.println("  解密后=" + new String(sm4DecryptGcm(key, nonce, gcmCipher)));
        gcmCipher[gcmCipher.length - 1] ^= 0x01;
        try {
            sm4DecryptGcm(key, nonce, gcmCipher);
            System.out.println("  篡改检测=未发现（异常！）");
        } catch (Exception e) {
            System.out.println("  篡改检测=失败于 " + e.getClass().getSimpleName());
        }

        System.out.println("[SM3] " + sm3Hex(sample.toBytes()) + "（64 字符，对标 SHA-256）");

        KeyPair keyPair = genSm2KeyPair();
        byte[] signature = sm2Sign(keyPair.getPrivate(), sample.toBytes());
        System.out.println("[SM2] 签名=" + HexUtil.toHex(signature).substring(0, 48) + "...  长度 " + signature.length + " 字节");
        System.out.println("  验签=" + sm2Verify(keyPair.getPublic(), sample.toBytes(), signature)
                + "，篡改后验签=" + sm2Verify(keyPair.getPublic(), (sample.plaintext() + "x").getBytes("UTF-8"), signature));
        byte[] sm2Cipher = sm2Encrypt(keyPair.getPublic(), sample.toBytes());
        System.out.println("  SM2 加密解密后=" + new String(sm2Decrypt(keyPair.getPrivate(), sm2Cipher)));
    }
}
