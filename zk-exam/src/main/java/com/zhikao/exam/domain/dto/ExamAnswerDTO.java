package com.zhikao.exam.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 单题作答表单
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "单题作答表单")
public class ExamAnswerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("题目id")
    private Long questionId;

    @ApiModelProperty("学生答案，可能是数字、布尔、字符串或数组")
    private Object answer;

    @ApiModelProperty("题目类型，后端以题目数据为准")
    private Integer questionType;
}
