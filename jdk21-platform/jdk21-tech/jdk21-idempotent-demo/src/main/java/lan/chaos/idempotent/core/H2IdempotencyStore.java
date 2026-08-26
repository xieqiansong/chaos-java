package lan.chaos.idempotent.core;

import lan.chaos.idempotent.common.constant.Scenario;
import lan.chaos.idempotent.common.model.IdempotencyRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * H2 内存去重表实现。
 *
 * WHY（生产坑）：单线程下「SELECT 存在否 → 不存在才 INSERT」有竞态——两个相同请求
 * 同时穿过 SELECT 都会认为自己首检成功，于是业务副作用执行两次。
 * 解法：依赖数据库唯一约束（key+scope 唯一），用 {@code INSERT} 的受影响行数判定首检：
 *   - 行数 == 1 → 我是首次，放行业务。
 *   - 抛唯一键冲突 / 行数 == 0 → 重复，返回首检 false。
 * 这在并发双发下依然成立，因为唯一约束由数据库保证原子性。
 *
 * 注意：本 demo 仅做机制演示，未引入事务传播复杂度；生产里「首检 INSERT」与「业务写」
 * 应放在同一事务（或同一把分布式锁）内，否则存在「首检成功但业务回滚、重复请求又进来」的窗口。
 * 该窗口在 {@code RequestIdempotentGuard} 的并发双发测试中通过事务内首检体现。
 */
@Repository
public class H2IdempotencyStore implements IdempotencyStore {

    private final JdbcTemplate jdbc;

    public H2IdempotencyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        jdbc.execute("CREATE TABLE IF NOT EXISTS idempotency_record (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "k VARCHAR(128) NOT NULL, " +
                "scope VARCHAR(32) NOT NULL, " +
                "biz_no VARCHAR(64), " +
                "created_at TIMESTAMP, " +
                "CONSTRAINT uk_key_scope UNIQUE (k, scope))");
    }

    @Override
    public boolean tryMarkFirst(IdempotencyRecord record) {
        try {
            int rows = jdbc.update(
                    "INSERT INTO idempotency_record(k, scope, biz_no, created_at) VALUES (?,?,?,?)",
                    record.getKey(), record.getScope(), record.getBizNo(),
                    record.getCreatedAt() == null ? LocalDateTime.now() : record.getCreatedAt());
            return rows == 1;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 唯一约束冲突 = 重复到达
            return false;
        }
    }

    @Override
    public boolean exists(String key, String scope) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(1) FROM idempotency_record WHERE k=? AND scope=?",
                Integer.class, key, scope);
        return n != null && n > 0;
    }

    @Override
    public void clear(String key, String scope) {
        jdbc.update("DELETE FROM idempotency_record WHERE k=? AND scope=?", key, scope);
    }
}
