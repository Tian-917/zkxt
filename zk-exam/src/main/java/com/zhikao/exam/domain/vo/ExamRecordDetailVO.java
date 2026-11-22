package com.zhikao.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 考试答题明细
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "考试答题明细")
public class ExamRecordDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("题目信息")
    private ExamQuestionVO question;

    @ApiModelProperty("学生答案，判断题返回布尔值，未作答返回空字符串")
    private Object answer;

    @ApiModelProperty("是否答对")
    private Boolean correct;

    @ApiModelProperty("本题得分")
    private Integer score;
}
