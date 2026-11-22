package com.zhikao.course.domain.dto;

import com.zhikao.course.constants.CourseErrorInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author Tian
 * @since 2026-11-19
 * @version 1.0.0
 **/
@ApiModel(description = "课程id")
@Data
public class CourseIdDTO {
    @ApiModelProperty("课程id")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_OPERATE_ID_NULL)
    private Long id;
}
