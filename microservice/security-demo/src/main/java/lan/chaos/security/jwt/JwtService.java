package lan.chaos.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lan.chaos.security.common.constant.SecurityConstant;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * ★★★ 高频：JWT（JSON Web Token）—— 无状态 Token 方案的核心。
 *
 * <p>痛点：传统 Session/Cookie 把登录态存服务端，集群需共享 Session（Redis），
 * 且跨域/移动端不友好。JWT 把「声明（subject/role/exp…）」自包含进签名 Token，
 * 服务端<b>无状态</b>校验签名即可，天然适合分布式/微服务。
 *
 * <p>关键 API（jjwt 0.11.x）：{@code Jwts.builder().setSubject().setExpiration().signWith(key, HS256).compact()}
 * 与 {@code Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody()}。
 *
 * <p>生产坑：
 * <ul>
 *   <li>密钥必须足够强（HS256 >= 256 位）且放 KMS，泄露即任何人可伪造 Token。</li>
 *   <li><b>JWT 无法主动吊销</b>：退出登录要靠短过期 + 刷新令牌，或维护黑名单。</li>
 *   <li>别把敏感信息放 payload（只是 Base64，非加密）。</li>
 *   <li>务必校验过期与签名，且用 HTTPS 传输防劫持。</li>
 *   <li>算法混淆攻击：固定用 HS256，别让客户端决定算法。</li>
 * </ul>
 */
public class JwtService {

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SecurityConstant.JWT_SECRET.getBytes());

    /** 签发 Token：subject + 过期秒数。 */
    public static String issue(String subject, long ttlSeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlSeconds * 1000))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析 Token 返回 subject；过期/篡改/签名错都会抛 {@link JwtException}。 */
    public static String parseSubject(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}
