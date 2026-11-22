package com.zhikao.exam.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 创建练习/考试表单
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "创建练习/考试表单")
public class ExamCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("课程id")
    private Long courseId;

    @ApiModelProperty("小节id")
    private Long sectionId;

    @ApiModelProperty("类型：1练习，2考试")
    private Integer type;
}
