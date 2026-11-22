-- 职考云 · 证书域（新增模块 zk-cert）
-- 目标库：zk_cert

CREATE DATABASE IF NOT EXISTS `zk_cert` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `zk_cert`;

-- 证书表：一个人、一场考试只能有一张证（uk_user_exam 是发证幂等的根）
CREATE TABLE IF NOT EXISTS `certificate` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cert_no`     VARCHAR(32)  NOT NULL COMMENT '证书编号：ZK-YYYY-NNNNNN',
  `user_id`     BIGINT       NOT NULL COMMENT '持证人id',
  `user_name`   VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '持证人姓名',
  `cert_type`   VARCHAR(32)  NOT NULL COMMENT '证书类型：初级会计/教资/软考',
  `exam_id`     BIGINT       NOT NULL COMMENT '来源考试记录id',
  `score`       INT          NOT NULL DEFAULT 0 COMMENT '考核成绩',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1已签发 2已作废',
  `issue_time`  DATETIME     NOT NULL COMMENT '签发时间',
  `expire_time` DATETIME     NOT NULL COMMENT '有效期至',
  `verify_code` CHAR(12)     NOT NULL COMMENT '验真短码',
  `notified`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已做到期提醒',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cert_no` (`cert_no`),
  UNIQUE KEY `uk_user_exam` (`user_id`, `exam_id`),
  UNIQUE KEY `uk_verify_code` (`verify_code`),
  KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书';

-- 本地消息/补偿表：发证失败落一条，由 XXL-Job 重试（最终一致）
CREATE TABLE IF NOT EXISTS `cert_task` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type`        VARCHAR(32)  NOT NULL COMMENT '业务类型：ISSUE',
  `biz_id`          BIGINT       NOT NULL COMMENT '业务id（考试记录id）',
  `payload`         VARCHAR(512) DEFAULT NULL COMMENT '失败时的原始消息，便于重放',
  `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1成功 2失败',
  `retry_count`     INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `next_retry_time` DATETIME     DEFAULT NULL COMMENT '下次重试时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发证补偿任务';
