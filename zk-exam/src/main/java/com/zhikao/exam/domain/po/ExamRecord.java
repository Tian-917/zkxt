package com.zhikao.exam.domain.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试记录（MongoDB）
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Document("exam_record")
public class ExamRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 考试记录id
     */
    @Id
    private String id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 小节id
     */
    private Long sectionId;

    /**
     * 课表id
     */
    private Long lessonId;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 小节名称
     */
    private String sectionName;

    /**
     * 类型：1练习，2考试
     */
    private Integer type;

    /**
     * 状态：0进行中，1已提交
     */
    private Integer status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 提交时间
     */
    private LocalDateTime commitTime;

    /**
     * 答题时长，单位秒
     */
    private Integer duration;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 题目id集合，顺序即试卷顺序
     */
    private List<Long> questionIds;

    /**
     * 答题明细
     */
    private List<ExamRecordDetail> examDetails;
}
