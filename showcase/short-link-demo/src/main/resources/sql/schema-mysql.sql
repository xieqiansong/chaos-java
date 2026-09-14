-- ============================================
-- 短链系统 - MySQL 建表脚本
-- 数据库: short_link
-- ============================================

CREATE DATABASE IF NOT EXISTS short_link
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE short_link;

-- 短链表
CREATE TABLE IF NOT EXISTS t_short_url (
    id           BIGINT       NOT NULL COMMENT 'Snowflake ID',
    short_key    VARCHAR(10)  NOT NULL COMMENT 'Base62 短链 Key',
    original_url TEXT         NOT NULL COMMENT '原始长链接',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expire_time  DATETIME     NULL     COMMENT '过期时间（空=永不过期）',

    PRIMARY KEY (id),
    UNIQUE INDEX idx_short_key (short_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短链映射表';
