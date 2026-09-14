-- ============ 表结构（H2 / MySQL 兼容模式） ============
-- 注意：user / order 都是 SQL 保留字，故表名加 t_ 前缀（t_user / t_order），
--       避免「FROM user」被数据库当作关键字报语法错；
--       deleted 给默认值 0，配合逻辑删除插件，避免初始数据被 WHERE deleted=0 过滤掉；
--       version 给默认值 1，配合乐观锁插件：插入后 version=1，首次更新自增为 2。
-- DROP TABLE IF EXISTS 保证重复初始化可安全重跑（H2 内存库每次 JVM 重建）。

DROP TABLE IF EXISTS t_user;
DROP TABLE IF EXISTS t_order;
DROP TABLE IF EXISTS tenant_data;
DROP TABLE IF EXISTS log_record_2024;
DROP TABLE IF EXISTS log_record_2025;
DROP TABLE IF EXISTS member;

CREATE TABLE t_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64),
    age         INT,
    email       VARCHAR(128),
    status      INT,
    tenant_id   BIGINT,
    version     INT DEFAULT 1,
    deleted     INT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    operator    VARCHAR(64)
);

CREATE TABLE t_order (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    amount      DECIMAL(12, 2),
    create_time TIMESTAMP
);

CREATE TABLE tenant_data (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT,
    biz_data  VARCHAR(255)
);

CREATE TABLE log_record_2024 (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    content     VARCHAR(255),
    create_time TIMESTAMP
);

CREATE TABLE log_record_2025 (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    content     VARCHAR(255),
    create_time TIMESTAMP
);

CREATE TABLE member (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(64),
    phone VARCHAR(255)
);
