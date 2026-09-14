package lan.chaos.crypto.symmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.Base64Util;
import lan.chaos.crypto.common.util.CryptoProviders;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * ★★ 推荐：ChaCha20-Poly1305 —— 流密码 ChaCha20 与 MAC Poly1305 组成的 AEAD。
 *
 * <p>痛点：AES-GCM 在没有 AES 硬件指令（AES-NI）的设备上会退化为软件实现、且容易遭受
 * 缓存时序攻击。ChaCha20 是纯 ARX（加-旋转-异或）设计，<b>软件实现也很快且天然抗时序攻击</b>，
 * 因此成为移动端 / 嵌入式 / TLS 1.3 的默认套件之一。
 *
 * <p>关键 API：{@code Cipher.getInstance("ChaCha20-Poly1305", "BC")}，
 * 密钥固定 <b>256 位</b>，nonce 推荐 <b>96 位（12 字节）</b>，认证标签 128 位。
 * JDK8 原生没有该算法，必须经 BouncyCastle 提供者。
 *
 * <p>生产坑：
 * <ul>
 *   <li>Poly1305 的密钥是「一次性」的——<b>同一密钥 + 同一 nonce 绝不可加密两段数据</b>，
 *       否则 MAC 密钥直接泄露。</li>
 *   <li>nonce 建议用计数器或随机数并保证全局唯一；随机 12 字节在单密钥加密次数 ≪ 2^32 时碰撞概率可忽略。</li>
 *   <li>JDK8 必须指定 {@code "BC"}，否则 {@code NoSuchAlgorithmException}。</li>
 * </ul>
 */
public class ChaCha20Demo {

    /** 转换串：BC 提供者下的 ChaCha20-Poly1305 AEAD。 */
    public static final String TRANSFORMATION = "ChaCha20-Poly1305";

    private static final String ALG = "ChaCha20";
    private static final int KEY_LEN = 32;
    private static final int NONCE_LEN = 12;
    private static final SecureRandom RNG = new SecureRandom();

    static {
        CryptoProviders.ensureBouncyCastle();
    }

    private ChaCha20Demo() {
    }

    /** 生成 256 位密钥。 */
    public static byte[] genKey() {
        byte[] key = new byte[KEY_LEN];
        RNG.nextBytes(key);
        return key;
    }

    /** 生成 96 位 nonce。 */
    public static byte[] genNonce() {
        byte[] nonce = new byte[NONCE_LEN];
        RNG.nextBytes(nonce);
        return nonce;
    }

    public static byte[] encrypt(byte[] key, byte[] nonce, byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, CryptoProviders.BC);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALG), new IvParameterSpec(nonce));
        return cipher.doFinal(plaintext);
    }

    public static byte[] decrypt(byte[] key, byte[] nonce, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, CryptoProviders.BC);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ALG), new IvParameterSpec(nonce));
        return cipher.doFinal(ciphertext);
    }

    public static void main(String[] args) throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] key = genKey();
        byte[] nonce = genNonce();

        byte[] ciphertext = encrypt(key, nonce, sample.toBytes());
        byte[] plaintext = decrypt(key, nonce, ciphertext);
        System.out.println("[ChaCha20-Poly1305] 密钥=" + key.length * 8 + " 位  nonce=" + nonce.length + " 字节");
        System.out.println("  密文(" + ciphertext.length + " 字节，含 16 字节 tag) Base64=" + Base64Util.encode(ciphertext));
        System.out.println("  解密后=" + new String(plaintext));

        ciphertext[ciphertext.length - 1] ^= 0x01;
        try {
            decrypt(key, nonce, ciphertext);
            System.out.println("  篡改检测=未发现（异常！）");
        } catch (Exception e) {
            System.out.println("  篡改检测=失败于 " + e.getClass().getSimpleName() + "（认证标签校验不通过）");
        }
    }
}
