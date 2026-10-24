package lan.chaos.oauth2.auth.authorize;

import lan.chaos.oauth2.auth.common.model.Scope;
import lan.chaos.oauth2.auth.common.constant.OAuth2Constant;
import lan.chaos.oauth2.auth.token.TokenStore;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Set;

/**
 * 授权码生成与消费（能力②）。强随机生成、单次消费、短 TTL。
 */
@Service
public class AuthorizationCodeService {

    private final TokenStore tokenStore;
    private final SecureRandom random = new SecureRandom();

    public AuthorizationCodeService(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public String issue(String clientId, String userId, Set<Scope> scopes,
                        String redirectUri, String codeChallenge) {
        String code = generateCode();
        Date expiry = new Date(System.currentTimeMillis() + OAuth2Constant.CODE_TTL_SECONDS * 1000L);
        tokenStore.saveCode(new CodeRecord(code, clientId, userId, scopes, redirectUri, codeChallenge, expiry));
        return code;
    }

    /** 单次消费：成功返回记录并标记 consumed；失败（缺失/已用/过期）抛异常。 */
    public CodeRecord consume(String code) {
        return tokenStore.consumeCode(code);
    }

    private String generateCode() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
