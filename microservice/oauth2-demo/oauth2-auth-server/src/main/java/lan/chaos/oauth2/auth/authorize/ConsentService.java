package lan.chaos.oauth2.auth.authorize;

import lan.chaos.oauth2.auth.common.constant.OAuth2Constant;
import lan.chaos.oauth2.auth.common.model.Scope;
import lan.chaos.oauth2.auth.token.TokenStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 授权确认（consent）服务（能力②补充）。
 *
 * <p>WHY：OAuth2 必须让用户明确「批准把哪些 scope 授权给这个应用」。
 * 本服务把申请 scope 转成可读文案（UI 该渲染的数据），并把待确认状态暂存；
 * 用户批准后才签发授权码。纯 API 形态下不渲染页面，直接返回结构化的 scope 描述。</p>
 */
@Service
public class ConsentService {

    private final TokenStore tokenStore;
    private final AuthorizationCodeService codeService;

    public ConsentService(TokenStore tokenStore, AuthorizationCodeService codeService) {
        this.tokenStore = tokenStore;
        this.codeService = codeService;
    }

    public ConsentRecord create(String clientId, String userId, Set<Scope> scopes,
                               String redirectUri, String state, String codeChallenge) {
        Date expiry = new Date(System.currentTimeMillis() + OAuth2Constant.CODE_TTL_SECONDS * 1000L);
        ConsentRecord record = new ConsentRecord(clientId, userId, scopes, redirectUri, state, codeChallenge, expiry);
        tokenStore.saveConsent(record);
        return record;
    }

    /** 用户批准：签发授权码。 */
    public String approve(String consentId) {
        ConsentRecord record = requirePending(consentId);
        record.setDecided(true);
        record.setApproved(true);
        return codeService.issue(record.getClientId(), record.getUserId(), record.getScopes(),
                record.getRedirectUri(), record.getCodeChallenge());
    }

    /** 用户拒绝。 */
    public void deny(String consentId) {
        ConsentRecord record = requirePending(consentId);
        record.setDecided(true);
        record.setApproved(false);
    }

    public ConsentRecord get(String consentId) {
        return tokenStore.getConsent(consentId);
    }

    /** scope → 可读文案列表（供客户端渲染授权确认页）。 */
    public List<Map<String, String>> describeScopes(Set<Scope> scopes) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Scope s : scopes) {
            Map<String, String> item = new HashMap<>();
            item.put("scope", s.getValue());
            item.put("description", s.getDescription());
            list.add(item);
        }
        return list;
    }

    private ConsentRecord requirePending(String consentId) {
        ConsentRecord record = tokenStore.getConsent(consentId);
        if (record == null) {
            throw new IllegalArgumentException("consent not found: " + consentId);
        }
        if (record.isDecided()) {
            throw new IllegalStateException("consent already decided");
        }
        if (record.isExpired()) {
            throw new IllegalStateException("consent expired");
        }
        return record;
    }
}
