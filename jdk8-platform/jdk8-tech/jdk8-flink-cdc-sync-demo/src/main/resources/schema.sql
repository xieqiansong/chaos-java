-- =========================================================================
-- Flink CDC 同源库表同步 Demo 建表语句
-- 源表：user / order    目标表：user_new / order_new
-- 字段与 UserMapping / OrderMapping 的 target 字段严格对应。
-- 用法：mysql -uroot -proot demo < schema.sql
-- =========================================================================

CREATE DATABASE IF NOT EXISTS demo DEFAULT CHARACTER SET utf8mb4;
USE demo;

-- 源表 user
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)  NOT NULL                COMMENT '用户名',
    `nickname`    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
    `status`      TINYINT      DEFAULT 1               COMMENT '状态',
    `age`         INT          DEFAULT NULL            COMMENT '年龄',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='源表：用户';

-- 目标表 user_new
DROP TABLE IF EXISTS `user_new`;
CREATE TABLE `user_new` (
    `id`          INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)  NOT NULL                COMMENT '用户名',
    `display_name` VARCHAR(64) DEFAULT NULL            COMMENT '昵称（源自 nickname）',
    `status`      TINYINT      DEFAULT 1               COMMENT '状态',
    `age`         INT          DEFAULT 0               COMMENT '年龄',
    `created_at`  DATETIME     DEFAULT NULL            COMMENT '创建时间（源自 create_time）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标表：用户';

-- 源表 order
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
    `order_id`    INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     INT          DEFAULT NULL            COMMENT '用户ID',
    `amount`      DECIMAL(10,2) DEFAULT NULL           COMMENT '金额',
    `state`       TINYINT      DEFAULT NULL            COMMENT '订单状态',
    `pay_time`    DATETIME     DEFAULT NULL            COMMENT '支付时间',
    PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='源表：订单';

-- 目标表 order_new
DROP TABLE IF EXISTS `order_new`;
CREATE TABLE `order_new` (
    `order_id`    INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     INT          DEFAULT NULL            COMMENT '用户ID',
    `amount`      DECIMAL(10,2) DEFAULT NULL           COMMENT '金额',
    `state`       TINYINT      DEFAULT NULL            COMMENT '订单状态',
    `paid_at`     DATETIME     DEFAULT NULL            COMMENT '支付时间（源自 pay_time）',
    `finished`    TINYINT      DEFAULT 0               COMMENT '是否完成（state=2 时置 1）',
    PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标表：订单';




-- 样例数据
delete from `user` where true;
delete from `order` where true;

delete from `user_new` where true;
delete from `order_new` where true;

INSERT INTO `user` (`username`, `nickname`, `status`, `age`, `create_time`) VALUES
    ('alice', 'Alice', 1, 30, NOW()),
    ('bob',   'Bob',   1, 25, NOW());

INSERT INTO `order` (`user_id`, `amount`, `state`, `pay_time`) VALUES
    (1, 99.90, 2, NOW()),   -- state=2 -> order_new.finished=1
    (1, 19.90, 1, NULL);    -- state=1 -> order_new.finished=0

SELECT * from `user_new`;
SELECT * from `order_new`;



