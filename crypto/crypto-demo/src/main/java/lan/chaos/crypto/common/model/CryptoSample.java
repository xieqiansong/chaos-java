package lan.chaos.crypto.common.model;

/**
 * 加密演示用的样例「明文」。所有方案都围绕同一段明文做加解密/签名，
 * 便于横向对比与断言「解密后 == 原文」。
 */
public class CryptoSample {

    private final String plaintext;

    public CryptoSample(String plaintext) {
        this.plaintext = plaintext;
    }

    public String plaintext() { return plaintext; }

    public byte[] toBytes() { return plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8); }

    /** 默认样例工厂：避免调用方自己准备输入。 */
    public static CryptoSample sampleSecret() {
        return new CryptoSample("secret: 订单金额=100&用户=Alice");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CryptoSample && plaintext.equals(((CryptoSample) o).plaintext);
    }

    @Override
    public int hashCode() {
        return plaintext.hashCode();
    }
}
