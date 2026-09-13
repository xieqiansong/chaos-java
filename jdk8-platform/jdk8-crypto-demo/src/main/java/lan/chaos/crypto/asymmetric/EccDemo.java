package lan.chaos.crypto.asymmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.HexUtil;

import javax.crypto.KeyAgreement;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

/**
 * ★★★ 推荐：椭圆曲线密码（ECC）—— 更短的密钥，同样的安全强度。
 *
 * <p>痛点：RSA 要 3072 位才等价于 128 位安全强度，密钥 / 签名 / 证书都很大，
 * 握手与存储成本高。ECC 用 256 位曲线即可达到同等强度，因此成为现代 TLS / 移动端的默认选择。
 *
 * <p>本类覆盖 ECC 的两大用途：
 * <ul>
 *   <li><b>ECDSA</b>（签名）：{@code SHA256withECDSA}，曲线 {@code secp256r1}（NIST P-256）。
 *       同一安全强度下 RSA 需 3072 位密钥、签名 384 字节，ECDSA 只要 256 位密钥、签名 ~72 字节。</li>
 *   <li><b>ECDH</b>（密钥协商）：双方各自生成密钥对，交换公钥后独立算出<b>同一个</b>共享秘密，
 *       再用它派生会话密钥。这是「前向保密（PFS）」的基础——每次会话换新密钥对，
 *       长期私钥泄露也解不开历史流量。</li>
 * </ul>
 *
 * <p>生产坑：
 *   <ul>
 *     <li>ECDSA 签名依赖高质量的随机数 k，k 复用会直接泄露私钥（索尼 PS3 事故）；
 *         要求「确定性、无随机数陷阱」就用 Ed25519（见 {@code Ed25519Demo}）。</li>
 *     <li>曲线选择：优先 P-256/P-384 或 Curve25519；自造 / 冷门曲线不要碰。</li>
 *     <li>协商出的共享秘密是原始字节，<b>不要直接当密钥用</b>，需经 HKDF 派生。</li>
 *   </ul>
 */
public class EccDemo {

    /** NIST P-256 曲线（等价于 128 位安全强度）。 */
    public static final String CURVE_P256 = "secp256r1";
    /** NIST P-384 曲线（等价于 192 位安全强度）。 */
    public static final String CURVE_P384 = "secp384r1";

    private static final SecureRandom RNG = new SecureRandom();

    private EccDemo() {
    }

    /** 生成 EC 密钥对（curve 取 {@link #CURVE_P256} / {@link #CURVE_P384}）。 */
    public static KeyPair genKeyPair(String curve) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec(curve), RNG);
        return generator.generateKeyPair();
    }

    // ------------------------------------------------------------------ ECDSA

    public static byte[] sign(PrivateKey privateKey, byte[] data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    public static boolean verify(PublicKey publicKey, byte[] data, byte[] signatureBytes) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }

    // ------------------------------------------------------------------ ECDH

    /**
     * ECDH 密钥协商：返回 32 字节共享秘密。
     * 双方各调一次（参数互换）会得到相同结果——这就是「不需要事先共享密钥」的密钥交换。
     */
    public static byte[] agreeSecret(PrivateKey myPrivate, PublicKey peerPublic) throws Exception {
        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(myPrivate);
        agreement.doPhase(peerPublic, true);
        return agreement.generateSecret();
    }

    public static void main(String[] args) throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();

        KeyPair keyPair = genKeyPair(CURVE_P256);
        byte[] signature = sign(keyPair.getPrivate(), sample.toBytes());
        System.out.println("[ECDSA/P-256] 私钥 " + ((java.security.interfaces.ECPrivateKey) keyPair.getPrivate()).getS().bitLength()
                + " 位，签名 " + signature.length + " 字节（RSA-3072 同强度签名要 384 字节）");
        System.out.println("  验签=" + verify(keyPair.getPublic(), sample.toBytes(), signature)
                + "，篡改后验签=" + verify(keyPair.getPublic(), (sample.plaintext() + "x").getBytes(), signature));

        // 两个参与方各自生成密钥对，交换公钥后算出同一共享秘密
        KeyPair alice = genKeyPair(CURVE_P256);
        KeyPair bob = genKeyPair(CURVE_P256);
        byte[] aliceSecret = agreeSecret(alice.getPrivate(), bob.getPublic());
        byte[] bobSecret = agreeSecret(bob.getPrivate(), alice.getPublic());
        System.out.println("[ECDH] Alice 算出=" + HexUtil.toHex(aliceSecret));
        System.out.println("       Bob   算出=" + HexUtil.toHex(bobSecret));
        System.out.println("       双方一致=" + HexUtil.toHex(aliceSecret).equals(HexUtil.toHex(bobSecret))
                + "（明文信道交换公钥即可，窃听者无法算出）");
    }
}
