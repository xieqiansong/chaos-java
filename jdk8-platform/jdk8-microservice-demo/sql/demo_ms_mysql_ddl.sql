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
