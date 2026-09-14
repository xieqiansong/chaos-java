CREATE TABLE IF NOT EXISTS `t_user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50),
  `age` INT,
  `email` VARCHAR(100),
  `status` INT,
  `tenant_id` BIGINT,
  `version` INT,
  `deleted` INT DEFAULT 0,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP,
  `operator` VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS `t_order` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT,
  `amount` DECIMAL(10,2),
  `create_time` TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `tenant_data` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `tenant_id` BIGINT,
  `biz_data` VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS `log_record_2024` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `content` VARCHAR(200),
  `create_time` TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `log_record_2025` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `content` VARCHAR(200),
  `create_time` TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `member` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50),
  `phone` VARCHAR(200)
);
