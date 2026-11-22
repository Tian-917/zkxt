-- 学习笔记相关表，数据库：zk_learning

USE zk_learning;

CREATE TABLE IF NOT EXISTS `note` (
    `id`               bigint       NOT NULL COMMENT '笔记id',
    `user_id`          bigint       NOT NULL COMMENT '用户id',
    `course_id`        bigint       NOT NULL COMMENT '课程id',
    `chapter_id`       bigint       NOT NULL COMMENT '章id',
    `section_id`       bigint       NOT NULL COMMENT '小节id',
    `note_moment`      int          NOT NULL DEFAULT '0' COMMENT '记录笔记时的视频播放时间，单位秒',
    `content`          text         NOT NULL COMMENT '笔记内容',
    `is_private`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否是隐私笔记，默认false',
    `hidden`           bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否被折叠（隐藏），默认false',
    `hidden_reason`    varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '被隐藏的原因',
    `author_id`        bigint       NOT NULL COMMENT '笔记作者id',
    `gathered_note_id` bigint       DEFAULT '0' COMMENT '被采集笔记的id',
    `is_gathered`      bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否是采集他人的笔记，默认false',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_section_id` (`section_id`) USING BTREE,
    KEY `idx_gathered_note_id` (`gathered_note_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT ='学习笔记';

CREATE TABLE IF NOT EXISTS `note_user` (
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `note_id`     bigint   NOT NULL COMMENT '引用的笔记id',
    `user_id`     bigint   NOT NULL COMMENT '引用者id',
    `is_gathered` bit(1)   NOT NULL DEFAULT b'0' COMMENT '是否是采集他人的笔记，默认false',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_note_id` (`note_id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    UNIQUE KEY `uk_note_user` (`note_id`, `user_id`, `is_gathered`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC COMMENT ='笔记引用信息';
