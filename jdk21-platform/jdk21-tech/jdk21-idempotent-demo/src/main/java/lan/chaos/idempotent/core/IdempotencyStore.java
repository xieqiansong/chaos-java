package lan.chaos.idempotent.core;

import lan.chaos.idempotent.common.model.IdempotencyRecord;

/**
 * 去重存储接口。实现用 H2（零外部依赖），真实项目可替换为 MySQL/Redis。
 * 关键方法 {@link #tryMarkFirst} 用「数据库唯一约束 + 受影响行数」做**原子首检**，
 * 这是并发双发下不重复执行副作用的基石。
 */
public interface IdempotencyStore {

    /**
     * 首检标记：尝试把去重键写入去重表。
     *
     * @return {@code true} 表示「我是首次」，调用方应继续执行真实业务副作用；
     *         {@code false} 表示「键已存在（重复到达）」，调用方应直接返回首检时的结果，禁止重复副作用。
     */
    boolean tryMarkFirst(IdempotencyRecord record);

    /** 判断某 key+scope 是否已存在（用于演示/对齐） */
    boolean exists(String key, String scope);

    /** 清理（TTL/归档演示位，当前按 key+scope 删除） */
    void clear(String key, String scope);
}
