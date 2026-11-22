package com.zhikao.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 我的考试列表项
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "我的考试列表项")
public class ExamPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("考试记录id")
    private String id;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("小节名称")
    private String sectionName;

    @ApiModelProperty("类型：0练习，1考试")
    private Integer type;

    @ApiModelProperty("得分")
    private Integer score;

    @ApiModelProperty("答题时长，单位秒")
    private Integer duration;

    @ApiModelProperty("提交时间")
    private LocalDateTime commitTime;
}
