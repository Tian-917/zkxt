package com.zhikao.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 考点维度的错题统计
 */
@Data
@Accessors(chain = true)
@ApiModel(description = "考点错题统计")
public class PointStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("小节id（考点）")
    private Long sectionId;

    @ApiModelProperty("考点名称")
    private String sectionName;

    @ApiModelProperty("错题数")
    private Integer questionCount;

    @ApiModelProperty("累计答错次数")
    private Integer wrongTimes;
}
