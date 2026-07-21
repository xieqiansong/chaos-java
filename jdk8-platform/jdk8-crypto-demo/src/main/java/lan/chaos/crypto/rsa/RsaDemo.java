package lan.chaos.crypto.rsa;

import lan.chaos.crypto.common.model.CryptoSample;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * ★★★ 高频：RSA 非对称加密 + 签名 —— 公钥加密/私钥解密，或私钥签名/公钥验签。
 *
 * <p>痛点：对称加密的密钥分发难题——双方没有安全信道交换密钥。RSA 用「公钥公开、私钥保密」，
 * 解决密钥分发；签名则解决「身份 + 防篡改」（私钥签名，任何人用公钥验签）。
 *
 * <p>关键 API：{@code KeyPairGenerator("RSA")}、{@code Cipher("RSA/ECB/PKCS1Padding")}（加密）、
 * {@code Signature("SHA256withRSA")}（签名）。
 *
 * <p>生产坑：
 * <ul>
 *   <li>RSA 加密有长度上限（与密钥长度相关），<b>绝不直接用 RSA 加密大块业务数据</b>；
 *       正确做法：RSA 加密「临时 AES 密钥」，业务数据用 AES 加密（混合加密）。</li>
 *   <li>明文填充必须用 OAEP（{@code RSA/ECB/OAEPWithSHA-256AndMGF1Padding}），避免 PKCS1 的缺陷。</li>
 *   <li>签名用 SHA256withRSA；私钥绝不能泄露，建议放 HSM/密钥库。</li>
 * </ul>
 */
public class RsaDemo {

    /** 生成 RSA 密钥对（默认 2048 位）。 */
    public static KeyPair genKeyPair(int bits) throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(bits);
        return g.generateKeyPair();
    }

    /** 公钥加密。 */
    public static byte[] encrypt(PublicKey pub, byte[] data) throws Exception {
        Cipher c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        c.init(Cipher.ENCRYPT_MODE, pub);
        return c.doFinal(data);
    }

    /** 私钥解密。 */
    public static byte[] decrypt(PrivateKey pri, byte[] data) throws Exception {
        Cipher c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        c.init(Cipher.DECRYPT_MODE, pri);
        return c.doFinal(data);
    }

    /** 私钥对数据签名。 */
    public static byte[] sign(PrivateKey pri, byte[] data) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(pri);
        sig.update(data);
        return sig.sign();
    }

    /** 公钥验签。 */
    public static boolean verify(PublicKey pub, byte[] data, byte[] signature) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pub);
        sig.update(data);
        return sig.verify(signature);
    }

    /** 控制台演示：加密往返 + 签名验签。 */
    public static void main(String[] args) throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();
        KeyPair kp = genKeyPair(2048);

        byte[] ct = encrypt(kp.getPublic(), s.toBytes());
        byte[] pt = decrypt(kp.getPrivate(), ct);
        System.out.printf("[RSA] 解密后=%s%n", new String(pt));

        byte[] sig = sign(kp.getPrivate(), s.toBytes());
        boolean ok = verify(kp.getPublic(), s.toBytes(), sig);
        System.out.printf("[RSA] 签名验签=%s%n", ok);
    }
}
