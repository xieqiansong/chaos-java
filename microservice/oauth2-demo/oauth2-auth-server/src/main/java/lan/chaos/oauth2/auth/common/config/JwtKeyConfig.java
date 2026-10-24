package lan.chaos.oauth2.auth.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * RS256 密钥对（演示用内存自签）。
 *
 * <p>WHY 用非对称 RS256 而非 HS256：授权服务器持有私钥签发，资源服务器只需公钥验签，
 * 公钥通过 {@code /.well-known/jwks.json} 分发，密钥不跨进程共享，扩展性更好。
 * 生产应把私钥放 KMS，并支持 {@code kid} 轮换（本 demo 固定 kid）。</p>
 */
@Configuration
public class JwtKeyConfig {

    private final KeyPair keyPair = generateRsaKey();

    @Bean
    public RSAPrivateKey jwtPrivateKey() {
        return (RSAPrivateKey) keyPair.getPrivate();
    }

    @Bean
    public RSAPublicKey jwtPublicKey() {
        return (RSAPublicKey) keyPair.getPublic();
    }

    @Bean
    public String jwtKid() {
        return "chaos-oauth2-demo-rsa-1";
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA not available", e);
        }
    }
}
