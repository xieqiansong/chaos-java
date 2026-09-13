package lan.chaos.crypto;

import lan.chaos.crypto.common.catalog.CryptoAlgorithmCatalog;

/**
 * 加密演示启动类（控制台 Runner）。
 *
 * <p>运行顺序：先打印「主流加密算法清单」作为导航，再依次分节执行各能力场景的
 * 「输入 → 输出」，纯内存、零外部服务依赖。想快速看效果就跑这个 main。
 */
public class CryptoApplication {

    public static void main(String[] args) throws Exception {
        CryptoAlgorithmCatalog.printCatalog();

        section("对称加密 · AES 全模式（ECB/CBC/CTR/CFB/OFB/GCM）");
        lan.chaos.crypto.symmetric.AesDemo.main(args);

        section("对称加密 · ChaCha20-Poly1305（无 AES 硬件加速时的首选 AEAD）");
        lan.chaos.crypto.symmetric.ChaCha20Demo.main(args);

        section("对称加密 · 遗留算法反面对照（DES / 3DES / RC4）");
        lan.chaos.crypto.symmetric.LegacyCipherDemo.main(args);

        section("非对称加密 · RSA（OAEP 加密 + PKCS1/PSS 签名 + 长度上限）");
        lan.chaos.crypto.asymmetric.RsaDemo.main(args);

        section("非对称加密 · 椭圆曲线 ECDSA 签名 + ECDH 密钥协商");
        lan.chaos.crypto.asymmetric.EccDemo.main(args);

        section("非对称加密 · DH 密钥协商（经典，对照 ECDH）");
        lan.chaos.crypto.asymmetric.DhDemo.main(args);

        section("非对称加密 · Ed25519 签名 + X25519 协商（现代曲线，JDK8 需 BC）");
        lan.chaos.crypto.asymmetric.Ed25519Demo.main(args);

        section("摘要 · MD5 / SHA-1 / SHA-2 / SHA3-256 对比");
        lan.chaos.crypto.digest.DigestDemo.main(args);

        section("消息认证码 · HMAC-SHA256 / HMAC-SHA1 / CMAC-AES");
        lan.chaos.crypto.digest.MacDemo.main(args);

        section("密钥派生 · PBKDF2（口令） + HKDF（共享秘密）");
        lan.chaos.crypto.kdf.KdfDemo.main(args);

        section("国密套件 · SM4 对称 / SM3 摘要 / SM2 非对称（BouncyCastle）");
        lan.chaos.crypto.sm.SmCryptoDemo.main(args);

        section("实战 · 混合加密（RSA-OAEP 包裹 AES-GCM 会话密钥）");
        lan.chaos.crypto.practice.HybridCryptoDemo.main(args);
    }

    private static void section(String title) {
        System.out.println("\n\n==================== " + title + " ====================");
    }
}
