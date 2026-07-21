package lan.chaos.crypto;

/**
 * 加密演示启动类：依次运行各方案的控制台「输入→输出」（AES / RSA / 摘要 / 国密）。
 */
public class CryptoApplication {
    public static void main(String[] args) throws Exception {
        System.out.println("===== AES =====");
        lan.chaos.crypto.aes.AesDemo.main(args);
        System.out.println("===== RSA =====");
        lan.chaos.crypto.rsa.RsaDemo.main(args);
        System.out.println("===== 摘要 SHA-256 =====");
        lan.chaos.crypto.digest.DigestDemo.main(args);
        System.out.println("===== 国密 SM2/SM3/SM4 =====");
        lan.chaos.crypto.sm.SmCryptoDemo.main(args);
    }
}
