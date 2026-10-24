package lan.chaos.oauth2.auth.token;

import lan.chaos.oauth2.auth.authorize.CodeRecord;
import lan.chaos.oauth2.auth.authorize.ConsentRecord;

/**
 * Token / Code / Consent 存储抽象。演示用内存实现；生产可换 Redis（用 TTL 自然过期 + 黑名单）。
 */
public interface TokenStore {

    // 授权码
    void saveCode(CodeRecord record);

    /** 单次消费：取出并标记 consumed，缺失/已用/过期抛异常。 */
    CodeRecord consumeCode(String code);

    // access（JWT 元数据 + 黑名单）
    void saveAccess(AccessRecord record);

    AccessRecord getAccess(String jti);

    void revokeAccess(String jti);

    boolean isAccessRevoked(String jti);

    // refresh
    void saveRefresh(RefreshRecord record);

    RefreshRecord getRefresh(String token);

    void revokeRefresh(String token);

    // consent
    void saveConsent(ConsentRecord record);

    ConsentRecord getConsent(String consentId);

    // 批量撤销
    void revokeByUser(String userId);

    void revokeByClient(String clientId);
}
