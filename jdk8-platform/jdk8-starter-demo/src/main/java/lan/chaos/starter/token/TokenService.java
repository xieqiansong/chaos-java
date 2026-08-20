package lan.chaos.starter.token;

import lan.chaos.starter.common.util.IdSampleFactory;

/**
 * Starter 提供的业务能力（功能仅作载体，重点是「它如何被自动装配出来」）。
 *
 * <p>职责：按 {@link TokenProperties} 配置生成一个带前缀的随机 token 字符串。</p>
 */
public class TokenService {

    private final TokenProperties properties;

    public TokenService(TokenProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成一个 token。输入依赖配置（length/prefix/charset），输出为拼接后字符串。
     */
    public String generate() {
        int length = Math.max(1, properties.getLength());
        StringBuilder sb = new StringBuilder(properties.getPrefix());
        String source = (properties.getCharset() == TokenProperties.Charset.HEX)
                ? "0123456789abcdef"
                : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String seed = IdSampleFactory.sampleUuid();
        for (int i = 0; i < length; i++) {
            int idx = (seed.charAt(i % seed.length()) + i) % source.length();
            sb.append(source.charAt(idx));
        }
        return sb.toString();
    }

    /** 暴露当前生效配置，便于「输入 → 输出」可观察。 */
    public TokenProperties getProperties() {
        return properties;
    }
}
