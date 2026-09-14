-- ============================================================
-- MySQL 副库 DDL：库名 demo_ms_mysql
-- 执行方式：在 MySQL 的 demo_ms_mysql 库中执行本文件
-- （mysql -h<host> -P<port> -u<user> -p demo_ms_mysql < demo_ms_mysql_ddl.sql）
-- ============================================================

CREATE TABLE IF NOT EXISTS t_user_tag (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED NOT NULL,
    tag        VARCHAR(64)  NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    INT          NOT NULL DEFAULT 0,
    KEY idx_user_tag_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户标签表（第二数据源 MySQL）';

-- ------------------------------------------------------------
-- P3 新增：订单表（Seata 分布式事务的「订单侧」分支，写在 MySQL）
-- 与用户账户余额（PG）分属不同库、不同服务，构成跨库跨服务全局事务。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_order (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    amount     DECIMAL(12,2) NOT NULL,
    status     VARCHAR(32)  NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表（演示 Seata 分布式事务分支资源）';

COMMENT ON COLUMN t_order.status IS 'CREATED=正常提交；ABORTED=全局回滚后不应出现（用于对照验证）';

-- ------------------------------------------------------------
-- P3 新增：Seata AT 模式 undo_log（每个参与全局事务的库都要有）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS undo_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    branch_id    BIGINT       NOT NULL,
    xid          VARCHAR(128) NOT NULL,
    context      VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB    NOT NULL,
    log_status   INT          NOT NULL,
    log_created  DATETIME(3)  NOT NULL,
    log_modified DATETIME(3)  NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT 模式回滚日志';
