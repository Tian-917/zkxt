package com.zhikao.exam.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 错题本
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wrong_question")
public class WrongQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学员id */
    private Long userId;

    /** 题目id */
    private Long questionId;

    /** 课程id */
    private Long courseId;

    /** 小节id（当作考点） */
    private Long sectionId;

    /** 考点名称 */
    private String sectionName;

    /** 最近一次作答 */
    private String lastAnswer;

    /** 累计答错次数 */
    private Integer wrongCount;

    /** 最近答错时间 */
    private LocalDateTime lastWrongTime;

    /** 0未掌握 1已掌握 */
    private Integer mastered;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
