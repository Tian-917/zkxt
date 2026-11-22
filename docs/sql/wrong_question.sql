-- 职考云 · 错题本（zk_exam 库，考试域子域）
-- 一题一条：uk(user_id, question_id)；答错累加次数，重练答对自动置为已掌握

CREATE TABLE IF NOT EXISTS `zk_exam`.`wrong_question` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         BIGINT       NOT NULL COMMENT '学员id',
  `question_id`     BIGINT       NOT NULL COMMENT '题目id',
  `course_id`       BIGINT       DEFAULT NULL COMMENT '课程id',
  `section_id`      BIGINT       DEFAULT NULL COMMENT '小节id（当作考点）',
  `section_name`    VARCHAR(128) DEFAULT NULL COMMENT '考点名称',
  `last_answer`     VARCHAR(64)  DEFAULT NULL COMMENT '最近一次作答',
  `wrong_count`     INT          NOT NULL DEFAULT 1 COMMENT '累计答错次数',
  `last_wrong_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近答错时间',
  `mastered`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0未掌握 1已掌握',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_user_mastered` (`user_id`, `mastered`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';
