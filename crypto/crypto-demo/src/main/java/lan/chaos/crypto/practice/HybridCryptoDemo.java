package lan.chaos.crypto.practice;

import lan.chaos.crypto.asymmetric.RsaDemo;
import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.Base64Util;
import lan.chaos.crypto.symmetric.AesDemo;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * ★★★ 实战：混合加密（信封加密）—— RSA 与 AES 各自的短板互补，也是 TLS / KMS 的通用做法。
 *
 * <p>为什么不能只用一种：
 * <ul>
 *   <li>只用 RSA：非对称运算慢，且有明文长度上限（2048 位 + OAEP-SHA256 仅 190 字节），没法加密业务数据。</li>
 *   <li>只用 AES：对称密钥怎么安全地给到对方？没有安全信道就死锁。</li>
 * </ul>
 *
 * <p>混合加密的做法（本类的 {@link Envelope}）：
 * <ol>
 *   <li>每次加密都<b>随机生成一把临时 AES-256 密钥</b>（会话密钥）；</li>
 *   <li>业务数据用 <b>AES-GCM</b> 加密（快、认证加密、不限长度）；</li>
 *   <li>把临时 AES 密钥用 <b>RSA-OAEP 公钥</b>加密，得到「被包裹的密钥（wrapped key）」；</li>
 *   <li>三者一起传给解密方：{@code wrappedKey + nonce + ciphertext}。</li>
 * </ol>
 * 解密方先用 RSA 私钥解出会话密钥，再解业务数据。这就是「数字信封」，
 * 也是 TLS 握手、KMS GenerateDataKey、JWE 的共同思想。
 *
 * <p>生产坑：
 * <ul>
 *   <li>会话密钥<b>必须每次随机、用完即弃</b>，绝不可复用（否则 GCM nonce 会复用，直接泄露明文）。</li>
 *   <li>真实系统里，包裹用的公钥来自证书 / KMS，私钥存在 HSM；不要把私钥写在配置文件里。</li>
 *   <li>若要「一对多」或「本地加密」，直接用 KMS 的数据密钥（Data Key）方案，别自己拼信封。</li>
 * </ul>
 */
public class HybridCryptoDemo {

    /** 数字信封：包裹密钥 + GCM nonce + 认证密文，三段都要一起传输。 */
    public static final class Envelope {
        private final byte[] wrappedKey;
        private final byte[] nonce;
        private final byte[] ciphertext;

        Envelope(byte[] wrappedKey, byte[] nonce, byte[] ciphertext) {
            this.wrappedKey = wrappedKey;
            this.nonce = nonce;
            this.ciphertext = ciphertext;
        }

        public byte[] wrappedKey() {
            return wrappedKey;
        }

        public byte[] nonce() {
            return nonce;
        }

        public byte[] ciphertext() {
            return ciphertext;
        }

        @Override
        public String toString() {
            return "Envelope{wrappedKey=" + wrappedKey.length + "B, nonce=" + nonce.length
                    + "B, ciphertext=" + ciphertext.length + "B}";
        }
    }

    private HybridCryptoDemo() {
    }

    /** 加密：RSA-OAEP 包裹随机 AES 密钥，AES-GCM 加密数据。 */
    public static Envelope encrypt(PublicKey rsaPublicKey, byte[] plaintext) throws Exception {
        byte[] sessionKey = AesDemo.genKey(256);
        byte[] nonce = AesDemo.genNonce();
        byte[] ciphertext = AesDemo.encryptGcm(sessionKey, nonce, plaintext);
        byte[] wrappedKey = RsaDemo.encryptOaep(rsaPublicKey, sessionKey);
        return new Envelope(wrappedKey, nonce, ciphertext);
    }

    /** 解密：RSA 私钥解出会话密钥，再 AES-GCM 解数据。 */
    public static byte[] decrypt(PrivateKey rsaPrivateKey, Envelope envelope) throws Exception {
        byte[] sessionKey = RsaDemo.decryptOaep(rsaPrivateKey, envelope.wrappedKey());
        return AesDemo.decryptGcm(sessionKey, envelope.nonce(), envelope.ciphertext());
    }

    public static void main(String[] args) throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = RsaDemo.genKeyPair(2048);

        Envelope envelope = encrypt(keyPair.getPublic(), sample.toBytes());
        System.out.println("[混合加密] " + envelope);
        System.out.println("  包裹密钥(RSA-OAEP 加密后)=" + Base64Util.encode(envelope.wrappedKey()).substring(0, 32) + "...");
        System.out.println("  业务密文(AES-GCM)长度=" + envelope.ciphertext().length
                + " 字节，与原文同数量级（RSA 单独加密只能 190 字节以内）");
        System.out.println("  解密后=" + new String(decrypt(keyPair.getPrivate(), envelope)));

        KeyPair attacker = RsaDemo.genKeyPair(2048);
        try {
            decrypt(attacker.getPrivate(), envelope);
            System.out.println("  错误私钥=未报错（异常！）");
        } catch (Exception e) {
            System.out.println("  错误私钥=" + e.getClass().getSimpleName() + "（RSA 解开包裹密钥即失败）");
        }

        System.out.println("\n[大文件] 1MB 数据混合加密：");
        byte[] big = new byte[1024 * 1024];
        for (int i = 0; i < big.length; i++) {
            big[i] = (byte) i;
        }
        Envelope bigEnvelope = encrypt(keyPair.getPublic(), big);
        byte[] restored = decrypt(keyPair.getPrivate(), bigEnvelope);
        System.out.println("  原文=" + big.length + " 字节  密文=" + bigEnvelope.ciphertext().length
                + " 字节  还原一致=" + java.util.Arrays.equals(big, restored));
    }
}
