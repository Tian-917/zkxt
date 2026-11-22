package com.zhikao.exam.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 提交试卷表单
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "提交试卷表单")
public class ExamSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("考试记录id")
    private String id;

    @ApiModelProperty("答题列表")
    private List<ExamAnswerDTO> examDetails;
}
