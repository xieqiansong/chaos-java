-- ============================================================
-- Seata Demo 数据库初始化脚本
-- 适用：H2 内存数据库（测试）+ MySQL（docker-compose）
-- ============================================================

-- ↓↓↓ AT 模式必需：Seata 回滚日志表 ↓↓↓
-- Seata 在执行业务 SQL 前会自动记录"前镜像"（SELECT FOR UPDATE），
-- 执行后记录"后镜像"，分支回滚时用 undo_log 恢复数据。
-- 生产环境建议对 undo_log 按天分区 + 定时清理。
CREATE TABLE IF NOT EXISTS undo_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME     NOT NULL,
    log_modified  DATETIME     NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
);

-- ↓↓↓ 业务表 ↓↓↓

-- 账户表：记录用户余额和 TCC 冻结金额
-- 冻结金额用于 TCC Try 阶段预留资源，Confirm 后转为实际扣减，Cancel 后解冻
CREATE TABLE IF NOT EXISTS account (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64)    NOT NULL,
    balance DECIMAL(10,2)  NOT NULL DEFAULT 0,
    frozen  DECIMAL(10,2)  NOT NULL DEFAULT 0,
    CONSTRAINT uk_account_user UNIQUE (user_id)
);

-- 订单表
CREATE TABLE IF NOT EXISTS order_tbl (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(64)   NOT NULL,
    product_id VARCHAR(64)   NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    status     INT           NOT NULL DEFAULT 0,
    order_no   VARCHAR(64)   NOT NULL,
    CONSTRAINT uk_order_no UNIQUE (order_no)
);

-- 库存表
CREATE TABLE IF NOT EXISTS storage (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL,
    total      INT         NOT NULL DEFAULT 0,
    frozen     INT         NOT NULL DEFAULT 0,
    CONSTRAINT uk_storage_product UNIQUE (product_id)
);

-- ↓↓↓ 种子数据：每次启动重置到初始状态 ↓↓↓
-- 用 MERGE 而非 INSERT 避免重复执行报错（H2 / MySQL 均兼容）
MERGE INTO account  (user_id, balance, frozen) KEY(user_id) VALUES ('U1001', 1000.00, 0);
MERGE INTO storage  (product_id, total, frozen) KEY(product_id) VALUES ('P1001', 100, 0);
