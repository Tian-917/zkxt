package com.zhikao.exam.constants;

/**
 * 考试相关常量
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface ExamConstants {

    /**
     * 类型：练习
     */
    int TYPE_PRACTICE = 1;

    /**
     * 类型：考试
     */
    int TYPE_EXAM = 2;

    /**
     * 状态：进行中，未提交
     */
    int STATUS_UNFINISHED = 0;

    /**
     * 状态：已提交
     */
    int STATUS_FINISHED = 1;

    /**
     * 及格线：正式考达到该分数且学时达标，才可签发证书
     */
    int PASS_SCORE = 60;
}
