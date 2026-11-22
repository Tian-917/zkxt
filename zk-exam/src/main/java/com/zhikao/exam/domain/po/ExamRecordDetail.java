package com.zhikao.exam.domain.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 考试记录明细，内嵌在考试记录中
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class ExamRecordDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 题目类型：1单选题，2多选题，3不定项选择题，4判断题，5主观题
     */
    private Integer questionType;

    /**
     * 学生答案，已归一化：选择题为逗号分隔的选项序号，判断题为1/0，主观题为原文
     */
    private String answer;

    /**
     * 是否答对
     */
    private Boolean correct;

    /**
     * 本题得分
     */
    private Integer score;
}
