-- ============================================
-- 秒杀系统 - PostgreSQL 建表脚本
-- 数据库: demo_seckill
-- ============================================

-- 创建数据库（如果不存在）—— 需要单独执行
-- CREATE DATABASE demo_seckill;

-- 商品表
CREATE TABLE IF NOT EXISTS t_product (
                                         id           BIGINT        NOT NULL,
                                         product_name VARCHAR(128)  NOT NULL,
    total_stock  INT           NOT NULL DEFAULT 0,
    bucket_count INT           NOT NULL DEFAULT 10,
    bucket_size  INT           NOT NULL DEFAULT 0,
    start_time   TIMESTAMPTZ   NULL,
    end_time     TIMESTAMPTZ   NULL,
    status       VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    version      INT           NOT NULL DEFAULT 0,
    create_time  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMPTZ   NULL,
    PRIMARY KEY (id)
    );

-- 商品表注释（单独添加）
COMMENT ON TABLE t_product IS '商品表';
COMMENT ON COLUMN t_product.id           IS '商品ID（由应用层生成）';
COMMENT ON COLUMN t_product.product_name IS '商品名称';
COMMENT ON COLUMN t_product.total_stock  IS '总库存';
COMMENT ON COLUMN t_product.bucket_count IS '分桶数量';
COMMENT ON COLUMN t_product.bucket_size  IS '每桶库存量（= total_stock / bucket_count）';
COMMENT ON COLUMN t_product.start_time   IS '秒杀开始时间';
COMMENT ON COLUMN t_product.end_time     IS '秒杀结束时间';
COMMENT ON COLUMN t_product.status       IS 'DRAFT=草稿, ACTIVE=秒杀中, SOLD_OUT=已售罄, CLOSED=已关闭';
COMMENT ON COLUMN t_product.version      IS '乐观锁版本号';
COMMENT ON COLUMN t_product.create_time  IS '创建时间';
COMMENT ON COLUMN t_product.update_time  IS '更新时间（由触发器或应用维护）';

-- 秒杀订单表
CREATE TABLE IF NOT EXISTS t_seckill_order (
                                               id           BIGINT         NOT NULL,
                                               product_id   BIGINT         NOT NULL,
                                               user_id      VARCHAR(64)    NOT NULL,
    token        VARCHAR(64)    NOT NULL,
    bucket_index INT            NOT NULL DEFAULT 0,
    status       VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
    amount       DECIMAL(10, 2) NOT NULL DEFAULT 0,
    create_time  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMPTZ    NULL,
    PRIMARY KEY (id)
    );

-- 订单表索引（单独创建）
CREATE UNIQUE INDEX IF NOT EXISTS idx_order_token ON t_seckill_order (token);
CREATE INDEX IF NOT EXISTS idx_order_user_product ON t_seckill_order (user_id, product_id);

-- 订单表注释
COMMENT ON TABLE t_seckill_order IS '秒杀订单表';
COMMENT ON COLUMN t_seckill_order.id           IS '订单ID（应用层生成）';
COMMENT ON COLUMN t_seckill_order.product_id   IS '商品ID';
COMMENT ON COLUMN t_seckill_order.user_id      IS '用户ID';
COMMENT ON COLUMN t_seckill_order.token        IS '秒杀令牌（唯一）';
COMMENT ON COLUMN t_seckill_order.bucket_index IS '库存分桶索引';
COMMENT ON COLUMN t_seckill_order.status       IS 'PENDING=待确认, CONFIRMED=已确认, CANCELLED=已取消';
COMMENT ON COLUMN t_seckill_order.amount       IS '订单金额';
COMMENT ON COLUMN t_seckill_order.create_time  IS '创建时间';
COMMENT ON COLUMN t_seckill_order.update_time  IS '更新时间';
