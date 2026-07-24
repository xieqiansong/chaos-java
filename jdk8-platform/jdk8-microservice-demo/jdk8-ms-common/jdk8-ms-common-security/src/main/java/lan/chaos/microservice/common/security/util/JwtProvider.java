package lan.chaos.microservice.common.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lan.chaos.microservice.common.security.constant.SecurityConstants;
import lan.chaos.microservice.common.security.model.LoginUser;
import lan.chaos.microservice.common.security.properties.JwtProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ★★★ P4 核心：JWT 签发与校验工具（jjwt 0.11.x）。
 *
 * <p>痛点：微服务集群下用 Session/Cookie 要共享会话（Redis），且对移动端/跨域不友好。
 * JWT 把「声明（uid/uname/perms/exp…）」自包含进带签名的 token，服务端<strong>无状态</strong>校验签名即可，
 * 天然适合网关统一鉴权 + 下游免查库拿身份。</p>
 *
 * <p>关键 API：
 * <ul>
 *   <li>签发：{@code Jwts.builder().setSubject().setExpiration().claim(...).signWith(key, HS256).compact()}</li>
 *   <li>校验：{@code Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody()}</li>
 * </ul>
 *
 * <p>生产坑：
 * <ul>
 *   <li>密钥必须够强（HS256 >= 256 位）且放 KMS，泄露即任意伪造。</li>
 *   <li><b>JWT 无法主动吊销</b>：退出登录靠「短 access + 可吊销 refresh」，或维护黑名单。</li>
 *   <li>payload 仅 Base64 编码非加密，别放密码等敏感信息。</li>
 *   <li>务必校验签名与过期；算法固定 HS256，防算法混淆攻击。</li>
 * </ul>
 */
public class JwtProvider {

    private final JwtProperties props;

    private final SecretKey key;

    public JwtProvider(JwtProperties props) {
        this.props = props;
        // 转成符合 HS256 长度要求的密钥对象；密钥过短 jjwt 会直接抛异常。
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 签发访问令牌（access）。 */
    public String generateAccessToken(LoginUser user) {
        return doGenerate(user, props.getAccessTokenTtl(), SecurityConstants.TOKEN_TYPE_ACCESS);
    }

    /** 签发刷新令牌（refresh，携带 jti 供服务端吊销）。 */
    public String generateRefreshToken(LoginUser user) {
        return doGenerate(user, props.getRefreshTokenTtl(), SecurityConstants.TOKEN_TYPE_REFRESH);
    }

    private String doGenerate(LoginUser user, long ttlMillis, String type) {
        long now = System.currentTimeMillis();
        String jti = UUID.randomUUID().toString();
        String perms = user.getPermissions() == null ? "" : String.join(",", user.getPermissions());
        return Jwts.builder()
                .setId(jti)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMillis))
                .claim(SecurityConstants.CLAIM_USER_ID, user.getUserId())
                .claim(SecurityConstants.CLAIM_USERNAME, user.getUsername())
                .claim(SecurityConstants.CLAIM_PERMISSIONS, perms)
                .claim(SecurityConstants.CLAIM_TOKEN_TYPE, type)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析 token 返回 Claims；过期/篡改/签名错都会抛 {@link JwtException}。 */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** 校验 token 是否合法（签名正确且未过期）。 */
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 取 token 的唯一 ID（jti），用于刷新令牌的服务端吊销比对。 */
    public String getJti(String token) {
        return parse(token).getId();
    }

    /** token 是否为访问令牌（防止拿 refresh 当 access 用）。 */
    public boolean isAccessToken(String token) {
        return SecurityConstants.TOKEN_TYPE_ACCESS.equals(parse(token).get(SecurityConstants.CLAIM_TOKEN_TYPE));
    }

    /** 从 token 还原 LoginUser（下游服务鉴权/取身份用）。 */
    public LoginUser getLoginUser(String token) {
        Claims c = parse(token);
        String perms = c.get(SecurityConstants.CLAIM_PERMISSIONS, String.class);
        Set<String> permSet = (perms == null || perms.isEmpty())
                ? Collections.emptySet()
                : Arrays.stream(perms.split(",")).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LoginUser user = new LoginUser();
        user.setUserId(c.get(SecurityConstants.CLAIM_USER_ID, Long.class));
        user.setUsername(c.getSubject());
        user.setPermissions(permSet);
        return user;
    }
}
