package com.zhikao.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题视图（复盘用：题干 + 选项 + 我的答案 + 正确答案 + 解析）
 */
@Data
@Accessors(chain = true)
@ApiModel(description = "错题详情")
public class WrongQuestionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("题目id")
    private Long questionId;

    @ApiModelProperty("题干")
    private String name;

    @ApiModelProperty("题型")
    private String type;

    @ApiModelProperty("选项")
    private List<String> options;

    @ApiModelProperty("我的答案")
    private String lastAnswer;

    @ApiModelProperty("正确答案")
    private String answer;

    @ApiModelProperty("解析")
    private String analysis;

    @ApiModelProperty("累计答错次数")
    private Integer wrongCount;

    @ApiModelProperty("最近答错时间")
    private LocalDateTime lastWrongTime;

    @ApiModelProperty("考点名称")
    private String sectionName;

    @ApiModelProperty("是否已掌握")
    private Boolean mastered;
}
