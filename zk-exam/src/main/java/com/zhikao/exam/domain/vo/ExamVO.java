package com.zhikao.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 创建练习/考试返回结果
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "创建练习/考试返回结果")
public class ExamVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("考试记录id")
    private String id;

    @ApiModelProperty("试卷题目列表")
    private List<ExamQuestionVO> questions;
}
