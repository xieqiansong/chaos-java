-- ============================================
-- 短链系统 - PostgreSQL 建表脚本
-- 数据库: short_link
-- ============================================

-- 短链表
CREATE TABLE IF NOT EXISTS t_short_url (
    id           BIGINT       NOT NULL,            -- Snowflake ID
    short_key    VARCHAR(10)  NOT NULL,            -- Base62 短链 Key
    original_url TEXT         NOT NULL,            -- 原始长链接
    create_time  TIMESTAMP    NOT NULL DEFAULT NOW(), -- 创建时间
    expire_time  TIMESTAMP    NULL,                -- 过期时间（空=永不过期）

    PRIMARY KEY (id),
    CONSTRAINT uk_short_key UNIQUE (short_key)
);

COMMENT ON TABLE  t_short_url      IS '短链映射表';
COMMENT ON COLUMN t_short_url.id           IS 'Snowflake ID';
COMMENT ON COLUMN t_short_url.short_key    IS 'Base62 短链 Key';
COMMENT ON COLUMN t_short_url.original_url IS '原始长链接';
COMMENT ON COLUMN t_short_url.create_time  IS '创建时间';
COMMENT ON COLUMN t_short_url.expire_time  IS '过期时间（空=永不过期）';

-- 索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_short_key ON t_short_url (short_key);
