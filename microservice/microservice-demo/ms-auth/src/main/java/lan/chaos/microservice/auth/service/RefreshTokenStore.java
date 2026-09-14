package lan.chaos.microservice.auth.service;

/**
 * 刷新令牌存储（吊销能力的关键）。
 *
 * <p>WHY 抽象成接口：access token 无状态、无法主动吊销；refresh token 存服务端，
 * 退出登录/风控时可删除使其失效，再逼 access 过期后必须重新登录。
 * 实现可插拔：{@code redis}（生产、可跨实例共享）或 {@code memory}（无 Redis 兜底）。</p>
 */
public interface RefreshTokenStore {

    /** 保存刷新令牌（key=用户+唯一 jti，带 TTL，到期自动清理）。 */
    void save(Long userId, String jti, long ttlSeconds);

    /** 刷新令牌是否仍然有效（存在且未过期）。 */
    boolean exists(Long userId, String jti);

    /** 删除某用户全部刷新令牌（登出即「踢下线」）。 */
    void removeAll(Long userId);
}
