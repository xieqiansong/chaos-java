package lan.chaos.crypto.sm;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.HexUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

/**
 * ★★★ 高频（合规场景）：国密算法 —— 中国商用密码标准，政务/金融/国企系统常要求。
 *
 * <p>三种核心国密（都由 BouncyCastle 提供者实现）：
 * <ul>
 *   <li><b>SM4</b>：对称分组密码（128 位密钥），对标 AES，用于数据加密。</li>
 *   <li><b>SM3</b>：哈希算法（256 位摘要），对标 SHA-256，用于完整性。</li>
 *   <li><b>SM2</b>：基于椭圆曲线的非对称算法，对标 RSA，用于加密/签名（本 demo 演示签名验签）。</li>
 * </ul>
 *
 * <p>关键 API（经 JCE + BC 提供者）：
 * <ul>
 *   <li>SM4：{@code Cipher.getInstance("SM4/CBC/PKCS5Padding", "BC")}</li>
 *   <li>SM3：{@code MessageDigest.getInstance("SM3", "BC")}</li>
 *   <li>SM2：{@code KeyPairGenerator.getInstance("SM2", "BC")} + {@code Signature.getInstance("SM3withSM2", "BC")}</li>
 * </ul>
 *
 * <p>生产坑：
 * <ul>
 *   <li>必须注册并指定 {@code "BC"} 提供者，否则 JDK 原生没有 SM2/SM3/SM4。</li>
 *   <li>SM2 签名用 {@code SM3withSM2}（对原文先 SM3 再 SM2 签名），验签方算法必须一致。</li>
 *   <li>SM4 同样需要随机 IV 且每次不同；合规场景推荐 SM4-CBC 或 SM4-GCM。</li>
 * </ul>
 */
public class SmCryptoDemo {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // ===== SM4 对称加密 =====
    public static byte[] genSm4Key() throws Exception {
        KeyGenerator g = KeyGenerator.getInstance("SM4", "BC");
        g.init(128);
        return g.generateKey().getEncoded();
    }

    public static byte[] sm4Encrypt(byte[] key, byte[] iv, byte[] plaintext) throws Exception {
        Cipher c = Cipher.getInstance("SM4/CBC/PKCS5Padding", "BC");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "SM4"), new IvParameterSpec(iv));
        return c.doFinal(plaintext);
    }

    public static byte[] sm4Decrypt(byte[] key, byte[] iv, byte[] ciphertext) throws Exception {
        Cipher c = Cipher.getInstance("SM4/CBC/PKCS5Padding", "BC");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "SM4"), new IvParameterSpec(iv));
        return c.doFinal(ciphertext);
    }

    // ===== SM3 摘要 =====
    public static String sm3Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SM3", "BC");
        return HexUtil.toHex(md.digest(data));
    }

    // ===== SM2 签名/验签 =====
    public static KeyPair genSm2KeyPair() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("SM2", "BC");
        g.initialize(new ECGenParameterSpec("sm2p256v1"), new SecureRandom());
        return g.generateKeyPair();
    }

    public static byte[] sm2Sign(PrivateKey pri, byte[] data) throws Exception {
        Signature sig = Signature.getInstance("SM3withSM2", "BC");
        sig.initSign(pri);
        sig.update(data);
        return sig.sign();
    }

    public static boolean sm2Verify(PublicKey pub, byte[] data, byte[] signature) throws Exception {
        Signature sig = Signature.getInstance("SM3withSM2", "BC");
        sig.initVerify(pub);
        sig.update(data);
        return sig.verify(signature);
    }

    public static void main(String[] args) throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();

        byte[] key = genSm4Key();
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        byte[] ct = sm4Encrypt(key, iv, s.toBytes());
        byte[] pt = sm4Decrypt(key, iv, ct);
        System.out.printf("[SM4] 密文=%s%n  解密后=%s%n", HexUtil.toHex(ct), new String(pt));

        System.out.printf("[SM3] %s%n", sm3Hex(s.toBytes()));

        KeyPair kp = genSm2KeyPair();
        byte[] sig = sm2Sign(kp.getPrivate(), s.toBytes());
        boolean ok = sm2Verify(kp.getPublic(), s.toBytes(), sig);
        System.out.printf("[SM2] 签名验签=%s%n", ok);
    }
}
