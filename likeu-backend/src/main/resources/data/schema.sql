-- ============================================
-- 建表脚本（MySQL 8 / H2 MySQL 兼容模式通用）
-- 全部使用 CREATE TABLE IF NOT EXISTS，可重复执行
-- ============================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS t_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  openid      VARCHAR(64)  NOT NULL,
  nickname    VARCHAR(64)  DEFAULT '',
  avatar      VARCHAR(512) DEFAULT '',
  word_book_id BIGINT      DEFAULT NULL,
  daily_new   INT          NOT NULL DEFAULT 20,
  daily_review INT         NOT NULL DEFAULT 50,
  create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 2. 词书表
CREATE TABLE IF NOT EXISTS t_word_book (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL,
  description VARCHAR(255) DEFAULT '',
  word_count  INT          NOT NULL DEFAULT 0,
  cover       VARCHAR(512) DEFAULT '',
  sort_order  INT          NOT NULL DEFAULT 0,
  create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 3. 单词表
-- 注意：MySQL 不允许 TEXT 列带 DEFAULT，故 TEXT 列均允许 NULL（默认值为 NULL）
CREATE TABLE IF NOT EXISTS t_word (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  word_book_id BIGINT      NOT NULL,
  word        VARCHAR(64)  NOT NULL,
  phonetic_uk VARCHAR(64)  DEFAULT '',
  phonetic_us VARCHAR(64)  DEFAULT '',
  audio_uk    VARCHAR(512) DEFAULT '',
  audio_us    VARCHAR(512) DEFAULT '',
  meaning_cn  TEXT         NOT NULL,
  example_en  TEXT         NULL,
  example_cn  TEXT         NULL,
  sort_order  INT          NOT NULL DEFAULT 0,
  create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 4. 词根词缀表
CREATE TABLE IF NOT EXISTS t_root (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  type        TINYINT      NOT NULL,
  root        VARCHAR(64)  NOT NULL,
  meaning     VARCHAR(128) NOT NULL,
  origin      VARCHAR(64)  DEFAULT '',
  example     VARCHAR(512) DEFAULT '',
  hot         INT          NOT NULL DEFAULT 0,
  create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 5. 单词-词根关联表
CREATE TABLE IF NOT EXISTS t_word_root (
  id          BIGINT   NOT NULL AUTO_INCREMENT,
  word_id     BIGINT   NOT NULL,
  root_id     BIGINT   NOT NULL,
  position    INT      NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 6. 用户单词学习记录表
CREATE TABLE IF NOT EXISTS t_user_word (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  user_id        BIGINT       NOT NULL,
  word_id        BIGINT       NOT NULL,
  status         TINYINT      NOT NULL DEFAULT 0,
  quality        TINYINT      NOT NULL DEFAULT 0,
  ease_factor    DECIMAL(5,2) NOT NULL DEFAULT 2.50,
  interval_days  INT          NOT NULL DEFAULT 0,
  repetitions    INT          NOT NULL DEFAULT 0,
  due_time       TIMESTAMP    DEFAULT NULL,
  last_review_time TIMESTAMP  DEFAULT NULL,
  review_count   INT          NOT NULL DEFAULT 0,
  wrong_count    INT          NOT NULL DEFAULT 0,
  create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 7. 用户收藏单词表
CREATE TABLE IF NOT EXISTS t_user_fav_word (
  id          BIGINT   NOT NULL AUTO_INCREMENT,
  user_id     BIGINT   NOT NULL,
  word_id     BIGINT   NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 8. 用户收藏词根表
CREATE TABLE IF NOT EXISTS t_user_fav_root (
  id          BIGINT   NOT NULL AUTO_INCREMENT,
  user_id     BIGINT   NOT NULL,
  root_id     BIGINT   NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 9. 每日学习统计表
CREATE TABLE IF NOT EXISTS t_user_daily (
  id          BIGINT   NOT NULL AUTO_INCREMENT,
  user_id     BIGINT   NOT NULL,
  study_date  DATE     NOT NULL,
  new_count   INT      NOT NULL DEFAULT 0,
  review_count INT     NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
