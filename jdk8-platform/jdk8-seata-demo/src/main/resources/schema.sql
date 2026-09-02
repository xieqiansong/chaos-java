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

-- ↓↓↓ SAGA 官方状态机引擎表（seata-saga-statelang，表前缀 seata_）↓↓↓
-- 引擎把状态机定义、执行实例、状态实例分别落库，支持服务端恢复与补偿。
-- 列定义与 seata-all 1.6.1 StateLangStoreSqls / StateLogStoreSqls 完全一致。

-- 状态机定义表：引擎启动时把 saga/*.json 解析后写入
CREATE TABLE IF NOT EXISTS seata_state_machine_def (
    id               VARCHAR(32)  NOT NULL,
    tenant_id        VARCHAR(32)  NOT NULL,
    app_name         VARCHAR(32)  NOT NULL,
    name             VARCHAR(128) NOT NULL,
    status           VARCHAR(2)   NOT NULL,
    gmt_create       DATETIME(3)  NOT NULL,
    ver              VARCHAR(16)  NOT NULL,
    type             VARCHAR(20),
    content          CLOB,
    recover_strategy VARCHAR(16),
    comment_         VARCHAR(255),
    PRIMARY KEY (id)
);

-- 状态机实例表：每次 start 记录一条执行实例
CREATE TABLE IF NOT EXISTS seata_state_machine_inst (
    id                  VARCHAR(128) NOT NULL,
    machine_id          VARCHAR(32)  NOT NULL,
    tenant_id           VARCHAR(32)  NOT NULL,
    parent_id           VARCHAR(128),
    business_key        VARCHAR(48),
    gmt_started         DATETIME(3)  NOT NULL,
    gmt_end             DATETIME(3),
    status              VARCHAR(2),
    compensation_status VARCHAR(2),
    is_running          TINYINT(1),
    gmt_updated         DATETIME(3)  NOT NULL,
    start_params        CLOB,
    end_params          CLOB,
    excep               BLOB,
    PRIMARY KEY (id),
    CONSTRAINT uk_smi_buz_tenant UNIQUE (business_key, tenant_id)
);

-- 状态实例表：每个 ServiceTask / 补偿步骤记录一条状态实例
CREATE TABLE IF NOT EXISTS seata_state_inst (
    id                       VARCHAR(48)  NOT NULL,
    machine_inst_id          VARCHAR(128) NOT NULL,
    name                     VARCHAR(128) NOT NULL,
    type                     VARCHAR(20),
    business_key             VARCHAR(48),
    gmt_started              DATETIME(3)  NOT NULL,
    service_name             VARCHAR(128),
    service_method           VARCHAR(128),
    service_type             VARCHAR(16),
    is_for_update            TINYINT(1),
    status                   VARCHAR(2)   NOT NULL,
    input_params             CLOB,
    output_params            CLOB,
    excep                    BLOB,
    gmt_end                  DATETIME(3),
    state_id_compensated_for VARCHAR(50),
    state_id_retried_for     VARCHAR(50),
    PRIMARY KEY (id, machine_inst_id)
);

-- ↓↓↓ 种子数据：每次启动重置到初始状态 ↓↓↓
-- 用 MERGE 而非 INSERT 避免重复执行报错（H2 / MySQL 均兼容）
MERGE INTO account  (user_id, balance, frozen) KEY(user_id) VALUES ('U1001', 1000.00, 0);
MERGE INTO storage  (product_id, total, frozen) KEY(product_id) VALUES ('P1001', 100, 0);
