package com.zhikao.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 试卷中的题目信息
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "试卷中的题目信息")
public class ExamQuestionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("题目id")
    private Long id;

    @ApiModelProperty("题目名称，题干")
    private String name;

    @ApiModelProperty("题目类型，1：单选题，2：多选题，3：不定向选择题，4：判断题，5：主观题")
    private Integer type;

    @ApiModelProperty("难易度，1：简单，2：中等，3：困难")
    private Integer difficulty;

    @ApiModelProperty("分值")
    private Integer score;

    @ApiModelProperty("选择题选项")
    private List<String> options;

    @ApiModelProperty("正确答案，判断题返回布尔值；创建考试时为null")
    private Object answer;

    @ApiModelProperty("答案解析，创建考试时为null")
    private String analysis;
}
