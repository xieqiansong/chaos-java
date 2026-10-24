package lan.chaos.oauth2.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lan.chaos.oauth2.auth.common.constant.OAuth2Constant;
import lan.chaos.oauth2.auth.common.model.Scope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * access_token 签发与解析（能力③）。access_token = JWT（RS256，自包含）。
 *
 * <p>claims：sub=用户、client_id=客户端、scope=权限、jti=唯一标识（用于黑名单）、
 * exp=过期（默认 30 分钟）、kid=密钥版本。JWT 自包含便于资源服务器无状态验签，
 * 但「无法立即撤销」——故配套黑名单（{@link TokenStore#revokeAccess}）。</p>
 */
@Service
public class AccessTokenIssuer {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String kid;
    private final TokenStore tokenStore;
    private final long ttlSeconds;

    public AccessTokenIssuer(RSAPrivateKey privateKey, RSAPublicKey publicKey, String kid,
                             TokenStore tokenStore,
                             @Value("${oauth2.access-token-ttl-seconds:" + OAuth2Constant.ACCESS_TOKEN_TTL_SECONDS + "}") long ttlSeconds) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.kid = kid;
        this.tokenStore = tokenStore;
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(String userId, String clientId, Set<Scope> scopes) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlSeconds * 1000L);
        String token = Jwts.builder()
                .setHeaderParam("kid", kid)
                .setSubject(userId)
                .claim("client_id", clientId)
                .claim("scope", Scope.join(scopes))
                .setId(jti)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(privateKey, io.jsonwebtoken.SignatureAlgorithm.RS256)
                .compact();
        tokenStore.saveAccess(new AccessRecord(jti, userId, clientId, scopes, expiry));
        return token;
    }

    /** 验签 + 解析（被篡改/过期/签名不符都会抛 JwtException）。 */
    public Claims parse(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
