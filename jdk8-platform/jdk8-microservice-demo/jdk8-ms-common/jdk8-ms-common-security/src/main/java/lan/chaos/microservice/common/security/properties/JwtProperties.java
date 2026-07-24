package lan.chaos.microservice.common.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置（@ConfigurationProperties 绑定 {@code ms.security.jwt.*}）。
 *
 * <p>WHY 单独抽配置：密钥、过期时间这类「部署相关」参数必须由环境变量/配置中心覆盖，
 * 不该硬编码在代码里。默认值是 demo 可用值，生产必须改。</p>
 */
@ConfigurationProperties(prefix = "ms.security.jwt")
public class JwtProperties {

    /**
     * 签名密钥。HS256 要求 >= 256 位（32 字节）。
     * 生产务必用高熵随机串并放 KMS/配置中心，泄露即任何人可伪造 token。
     */
    private String secret = "REDACTED-abcdefghijklmnop";

    /** 访问令牌有效期（毫秒），默认 30 分钟——短命是为了「泄露影响面小」。 */
    private long accessTokenTtl = 30 * 60 * 1000L;

    /** 刷新令牌有效期（毫秒），默认 7 天——长命但存服务端可吊销。 */
    private long refreshTokenTtl = 7 * 24 * 60 * 60 * 1000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(long accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public long getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(long refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }
}
