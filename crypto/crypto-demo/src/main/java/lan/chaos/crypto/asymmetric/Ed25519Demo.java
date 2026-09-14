package lan.chaos.crypto.asymmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.CryptoProviders;
import lan.chaos.crypto.common.util.HexUtil;

import javax.crypto.KeyAgreement;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * ★★★ 推荐：现代椭圆曲线 —— Ed25519（签名）与 X25519（密钥协商）。
 *
 * <p>WHY 单独一类：ECDSA / ECDH 是「NIST 曲线」路线，而 Ed25519 / X25519 是
 * Daniel J. Bernstein 设计的 Curve25519 路线，特点是把工程陷阱从 API 层面消掉：
 * <ul>
 *   <li><b>Ed25519</b>：EdDSA 签名，<b>确定性</b>（不依赖随机数 k），
 *       因此不会像 ECDSA 那样因随机数复用而泄露私钥；签名 64 字节，验证极快。</li>
 *   <li><b>X25519</b>：Curve25519 上的密钥协商，实现短小、抗侧信道，是 TLS 1.3 的默认套件。</li>
 * </ul>
 *
 * <p>JDK8 原生<b>不含</b> Ed25519 / X25519（JDK15 才内置），因此这里必须用 BouncyCastle 提供者。
 *
 * <p>生产坑：
 * <ul>
 *   <li>必须注册并指定 {@code "BC"}，否则 {@code NoSuchAlgorithmException}。</li>
 *   <li>Ed25519 是纯签名算法，<b>不能用来加密</b>；要加密请用 X25519 协商 + AES-GCM。</li>
 *   <li>曲线固定 256 位，无需也无法选长度——不要拿「RSA 思维」去找 4096 位版本。</li>
 * </ul>
 */
public class Ed25519Demo {

    /** 签名算法名（BouncyCastle 提供）。 */
    public static final String ED25519 = "Ed25519";
    /** 密钥协商算法名（BouncyCastle 提供）。 */
    public static final String X25519 = "X25519";

    static {
        CryptoProviders.ensureBouncyCastle();
    }

    private Ed25519Demo() {
    }

    // ------------------------------------------------------------------ Ed25519 签名

    public static KeyPair genEd25519KeyPair() throws Exception {
        return KeyPairGenerator.getInstance(ED25519, CryptoProviders.BC).generateKeyPair();
    }

    public static byte[] sign(PrivateKey privateKey, byte[] data) throws Exception {
        Signature signature = Signature.getInstance(ED25519, CryptoProviders.BC);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    public static boolean verify(PublicKey publicKey, byte[] data, byte[] signatureBytes) throws Exception {
        Signature signature = Signature.getInstance(ED25519, CryptoProviders.BC);
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }

    // ------------------------------------------------------------------ X25519 协商

    public static KeyPair genX25519KeyPair() throws Exception {
        return KeyPairGenerator.getInstance(X25519, CryptoProviders.BC).generateKeyPair();
    }

    public static byte[] agreeSecret(PrivateKey myPrivate, PublicKey peerPublic) throws Exception {
        KeyAgreement agreement = KeyAgreement.getInstance(X25519, CryptoProviders.BC);
        agreement.init(myPrivate);
        agreement.doPhase(peerPublic, true);
        return agreement.generateSecret();
    }

    public static void main(String[] args) throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();

        KeyPair keyPair = genEd25519KeyPair();
        byte[] signature = sign(keyPair.getPrivate(), sample.toBytes());
        System.out.println("[Ed25519] 公钥 32 字节 / 签名 " + signature.length + " 字节");
        System.out.println("  验签=" + verify(keyPair.getPublic(), sample.toBytes(), signature)
                + "，篡改后验签=" + verify(keyPair.getPublic(), (sample.plaintext() + "x").getBytes(), signature));
        System.out.println("  同一数据两次签名是否相同=" + HexUtil.toHex(sign(keyPair.getPrivate(), sample.toBytes()))
                .equals(HexUtil.toHex(signature)) + "（确定性签名，不依赖随机数）");

        KeyPair alice = genX25519KeyPair();
        KeyPair bob = genX25519KeyPair();
        byte[] aliceSecret = agreeSecret(alice.getPrivate(), bob.getPublic());
        byte[] bobSecret = agreeSecret(bob.getPrivate(), alice.getPublic());
        System.out.println("[X25519] 共享秘密=" + HexUtil.toHex(aliceSecret));
        System.out.println("  双方一致=" + HexUtil.toHex(aliceSecret).equals(HexUtil.toHex(bobSecret)));
    }
}
