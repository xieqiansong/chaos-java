package lan.chaos.oauth2.auth.token;

import lan.chaos.oauth2.auth.common.constant.OAuth2Constant;
import lan.chaos.oauth2.auth.common.model.Scope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * refresh_token 生成与管理（能力③补充）。refresh = 不透明随机串（非 JWT），元数据落库。
 *
 * <p>滚动刷新（rolling）：每次刷新都签发「新的 refresh + 新的 access」，并使旧 refresh 失效，
 * 防止旧 refresh 泄露后被长期滥用。familyId 串联同一令牌族，便于整族失效（如改密）。</p>
 */
@Service
public class RefreshTokenService {

    private final TokenStore tokenStore;
    private final SecureRandom random = new SecureRandom();
    private final long ttlSeconds;

    public RefreshTokenService(TokenStore tokenStore,
                              @Value("${oauth2.refresh-token-ttl-seconds:" + OAuth2Constant.REFRESH_TOKEN_TTL_SECONDS + "}") long ttlSeconds) {
        this.tokenStore = tokenStore;
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(String userId, String clientId, Set<Scope> scopes, String familyId) {
        if (familyId == null) {
            familyId = UUID.randomUUID().toString().replace("-", "");
        }
        String token = generateToken();
        Date expiry = new Date(System.currentTimeMillis() + ttlSeconds * 1000L);
        tokenStore.saveRefresh(new RefreshRecord(token, familyId, userId, clientId, scopes, expiry));
        return token;
    }

    public RefreshRecord get(String token) {
        return tokenStore.getRefresh(token);
    }

    public void revoke(String token) {
        tokenStore.revokeRefresh(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder("rft_");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
