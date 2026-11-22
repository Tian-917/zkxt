-- 职考云 · 学时相关种子（由 tools/seed/gen_study_records.py 生成）

-- 1. 小节时长放大 8 倍（每节从 4 分钟变成 30 分钟量级），按课程判断是否已放大
UPDATE `zk_course`.`course_catalogue` c JOIN (SELECT course_id, MAX(media_duration) m FROM `zk_course`.`course_catalogue`       WHERE type = 2 GROUP BY course_id) t ON t.course_id = c.course_id SET c.media_duration = c.media_duration * 8 WHERE c.type = 2 AND c.media_duration > 0 AND t.m < 900;

-- 2. user_id=1 在课程1 的 6 个已完成小节
DELETE FROM `zk_learning`.`learning_record` WHERE user_id = 1;
INSERT INTO `zk_learning`.`learning_record` (id, lesson_id, section_id, user_id, moment, finished, create_time, finish_time) VALUES (1900000000000000400, 1900000000000000300, 3, 1, 2112, b'1', NOW(), NOW());
INSERT INTO `zk_learning`.`learning_record` (id, lesson_id, section_id, user_id, moment, finished, create_time, finish_time) VALUES (1900000000000000401, 1900000000000000300, 4, 1, 2112, b'1', NOW(), NOW());
INSERT INTO `zk_learning`.`learning_record` (id, lesson_id, section_id, user_id, moment, finished, create_time, finish_time) VALUES (1900000000000000402, 1900000000000000300, 5, 1, 2112, b'1', NOW(), NOW());
INSERT INTO `zk_learning`.`learning_record` (id, lesson_id, section_id, user_id, moment, finished, create_time, finish_time) VALUES (1900000000000000403, 1900000000000000300, 6, 1, 2112, b'1', NOW(), NOW());
INSERT INTO `zk_learning`.`learning_record` (id, lesson_id, section_id, user_id, moment, finished, create_time, finish_time) VALUES (1900000000000000404, 1900000000000000300, 7, 1, 2112, b'1', NOW(), NOW());
INSERT INTO `zk_learning`.`learning_record` (id, lesson_id, section_id, user_id, moment, finished, create_time, finish_time) VALUES (1900000000000000405, 1900000000000000300, 8, 1, 2112, b'1', NOW(), NOW());

-- 累计学时约 211 分钟（证书域要求 120）