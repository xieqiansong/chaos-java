package lan.chaos.oauth2.auth.token;

import lan.chaos.oauth2.auth.authorize.CodeRecord;
import lan.chaos.oauth2.auth.authorize.ConsentRecord;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版 TokenStore（默认实现）。零外部依赖，CI 自包含。
 * 生产可用 Redis 实现：access/refresh 用 EXPIRE 自然过期，撤销即写入黑名单 key。
 */
@Repository
public class InMemoryTokenStore implements TokenStore {

    private final Map<String, CodeRecord> codes = new ConcurrentHashMap<>();
    private final Map<String, AccessRecord> accesses = new ConcurrentHashMap<>();
    private final Map<String, RefreshRecord> refreshes = new ConcurrentHashMap<>();
    private final Map<String, ConsentRecord> consents = new ConcurrentHashMap<>();

    @Override
    public void saveCode(CodeRecord record) {
        codes.put(record.getCode(), record);
    }

    @Override
    public CodeRecord consumeCode(String code) {
        CodeRecord record = codes.get(code);
        if (record == null) {
            throw new IllegalStateException("invalid authorization code");
        }
        if (record.isConsumed()) {
            throw new IllegalStateException("authorization code already used");
        }
        if (record.isExpired()) {
            codes.remove(code);
            throw new IllegalStateException("authorization code expired");
        }
        record.setConsumed(true);
        return record;
    }

    @Override
    public void saveAccess(AccessRecord record) {
        accesses.put(record.getJti(), record);
    }

    @Override
    public AccessRecord getAccess(String jti) {
        return accesses.get(jti);
    }

    @Override
    public void revokeAccess(String jti) {
        AccessRecord record = accesses.get(jti);
        if (record != null) {
            record.setRevoked(true);
        }
    }

    @Override
    public boolean isAccessRevoked(String jti) {
        AccessRecord record = accesses.get(jti);
        return record != null && record.isRevoked();
    }

    @Override
    public void saveRefresh(RefreshRecord record) {
        refreshes.put(record.getToken(), record);
    }

    @Override
    public RefreshRecord getRefresh(String token) {
        return refreshes.get(token);
    }

    @Override
    public void revokeRefresh(String token) {
        RefreshRecord record = refreshes.get(token);
        if (record != null) {
            record.setRevoked(true);
        }
    }

    @Override
    public void saveConsent(ConsentRecord record) {
        consents.put(record.getConsentId(), record);
    }

    @Override
    public ConsentRecord getConsent(String consentId) {
        return consents.get(consentId);
    }

    @Override
    public void revokeByUser(String userId) {
        accesses.values().stream()
                .filter(r -> userId.equals(r.getUserId()))
                .forEach(r -> r.setRevoked(true));
        refreshes.values().stream()
                .filter(r -> userId.equals(r.getUserId()))
                .forEach(r -> r.setRevoked(true));
    }

    @Override
    public void revokeByClient(String clientId) {
        accesses.values().stream()
                .filter(r -> clientId.equals(r.getClientId()))
                .forEach(r -> r.setRevoked(true));
        refreshes.values().stream()
                .filter(r -> clientId.equals(r.getClientId()))
                .forEach(r -> r.setRevoked(true));
    }
}
