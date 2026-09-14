package lan.chaos.crypto.asymmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.CryptoProviders;
import lan.chaos.crypto.common.util.HexUtil;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

/**
 * ★★★ 高频：RSA 非对称加密 + 签名 —— 公钥公开、私钥保密。
 *
 * <p>痛点：对称加密的密钥分发难题——双方没有安全信道交换密钥。RSA 用「公钥加密、私钥解密」
 * 解决密钥分发；用「私钥签名、公钥验签」解决身份与防篡改。
 *
 * <p>两条能力、四种转换串：
 * <ul>
 *   <li><b>加密</b>：{@code RSA/ECB/OAEPWithSHA-256AndMGF1Padding}（推荐，语义安全）
 *       对照 {@code RSA/ECB/PKCS1Padding}（遗留，有 Bleichenbacher 填充预言风险）。</li>
 *   <li><b>签名</b>：{@code SHA256withRSA}（PKCS#1 v1.5，兼容性最好）
 *       对照 {@code RSASSA-PSS}（推荐，概率签名、可证明安全）。</li>
 * </ul>
 *
 * <p>生产坑：
 * <ul>
 *   <li><b>RSA 有明文长度上限</b>：2048 位 + OAEP-SHA256 时最多加密
 *       {@code 2048/8 - 2*32 - 2 = 190} 字节。<b>绝不能用 RSA 直接加密业务大块数据</b>；
 *       正确做法是混合加密：RSA 只包一把临时 AES 密钥，数据用 AES-GCM（见
 *       {@code practice/HybridCryptoDemo}，也是 TLS 的思路）。</li>
 *   <li>加密填充必须 OAEP，签名优先 PSS；老的 PKCS1 v1.5 仅用于兼容老系统。</li>
 *   <li>私钥放 HSM / 密钥库，绝不硬编码；密钥至少 2048 位（2030 年前建议升到 3072+）。</li>
 * </ul>
 */
public class RsaDemo {

    /** OAEP 填充的加密转换串（推荐）。 */
    public static final String TRANSFORM_OAEP = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    /** PKCS#1 v1.5 填充的加密转换串（遗留，仅兼容用）。 */
    public static final String TRANSFORM_PKCS1 = "RSA/ECB/PKCS1Padding";

    private static final String OAEP_HASH = "SHA-256";
    private static final int OAEP_HASH_LEN = 32;

    static {
        CryptoProviders.ensureBouncyCastle();
    }

    private RsaDemo() {
    }

    /** 生成 RSA 密钥对（bits 建议 2048 / 3072 / 4096）。 */
    public static KeyPair genKeyPair(int bits) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(bits);
        return generator.generateKeyPair();
    }

    // ------------------------------------------------------------------ 加密

    /** 公钥加密（OAEP，推荐）。 */
    public static byte[] encryptOaep(PublicKey publicKey, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORM_OAEP);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /** 私钥解密（OAEP，推荐）。 */
    public static byte[] decryptOaep(PrivateKey privateKey, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORM_OAEP);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    /** 公钥加密（PKCS#1 v1.5，遗留）。 */
    public static byte[] encryptPkcs1(PublicKey publicKey, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORM_PKCS1);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /** 私钥解密（PKCS#1 v1.5，遗留）。 */
    public static byte[] decryptPkcs1(PrivateKey privateKey, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORM_PKCS1);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    /** OAEP-SHA256 下 RSA 单次可加密的最大明文长度（超出即抛异常）。 */
    public static int oaepMaxPlaintextBytes(int keyBits) {
        return keyBits / 8 - 2 * OAEP_HASH_LEN - 2;
    }

    // ------------------------------------------------------------------ 签名

    /** 私钥签名（PKCS#1 v1.5）。 */
    public static byte[] sign(PrivateKey privateKey, byte[] data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    /** 公钥验签（PKCS#1 v1.5）。 */
    public static boolean verify(PublicKey publicKey, byte[] data, byte[] signatureBytes) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }

    /** 私钥签名（RSASSA-PSS，推荐；概率签名，同一数据每次签名都不同）。 */
    public static byte[] signPss(PrivateKey privateKey, byte[] data) throws Exception {
        Signature signature = Signature.getInstance("RSASSA-PSS", CryptoProviders.BC);
        signature.setParameter(new PSSParameterSpec(OAEP_HASH, "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    /** 公钥验签（RSASSA-PSS，参数必须与签名侧一致）。 */
    public static boolean verifyPss(PublicKey publicKey, byte[] data, byte[] signatureBytes) throws Exception {
        Signature signature = Signature.getInstance("RSASSA-PSS", CryptoProviders.BC);
        signature.setParameter(new PSSParameterSpec(OAEP_HASH, "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }

    // ------------------------------------------------------------------ 演示

    public static void main(String[] args) throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = genKeyPair(2048);

        byte[] ciphertext = encryptOaep(keyPair.getPublic(), sample.toBytes());
        byte[] plaintext = decryptOaep(keyPair.getPrivate(), ciphertext);
        System.out.println("[RSA-OAEP] 密文=" + HexUtil.toHex(ciphertext).substring(0, 48) + "...");
        System.out.println("  解密后=" + new String(plaintext));

        byte[] pkcs1 = encryptPkcs1(keyPair.getPublic(), sample.toBytes());
        System.out.println("[RSA-PKCS1(遗留)] 解密后=" + new String(decryptPkcs1(keyPair.getPrivate(), pkcs1)));

        byte[] signature = sign(keyPair.getPrivate(), sample.toBytes());
        System.out.println("[SHA256withRSA] 签名验签=" + verify(keyPair.getPublic(), sample.toBytes(), signature)
                + "，篡改后验签=" + verify(keyPair.getPublic(), (sample.plaintext() + "x").getBytes(), signature));

        byte[] pssSignature = signPss(keyPair.getPrivate(), sample.toBytes());
        System.out.println("[RSASSA-PSS] 签名验签=" + verifyPss(keyPair.getPublic(), sample.toBytes(), pssSignature));

        int limit = oaepMaxPlaintextBytes(2048);
        System.out.println("[RSA 长度上限] 2048 位 + OAEP-SHA256 单次最多 " + limit + " 字节；"
                + "超长数据请改用混合加密（RSA 包 AES 密钥）");
        try {
            encryptOaep(keyPair.getPublic(), new byte[limit + 1]);
            System.out.println("  超长加密=未报错（异常！）");
        } catch (Exception e) {
            System.out.println("  超长加密=" + e.getClass().getSimpleName() + "（数据过长，必须走混合加密）");
        }
    }
}
