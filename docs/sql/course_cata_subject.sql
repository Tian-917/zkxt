-- 课程-题目正式关系表，数据库：zk_course
-- 课程发布时需要从 course_cata_subject_draft 复制到该表

USE zk_course;

CREATE TABLE IF NOT EXISTS `course_cata_subject` (
    `id`         bigint NOT NULL AUTO_INCREMENT COMMENT '小节题目关系id',
    `course_id`  bigint DEFAULT NULL,
    `cata_id`    bigint NOT NULL COMMENT '小节id',
    `subject_id` bigint NOT NULL COMMENT '题目id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_course_id` (`course_id`) USING BTREE,
    KEY `idx_cata_id` (`cata_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='课程-题目关系表';
