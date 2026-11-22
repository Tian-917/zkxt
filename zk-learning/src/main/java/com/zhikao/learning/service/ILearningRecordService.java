package com.zhikao.learning.service;

import com.zhikao.api.dto.leanring.LearningLessonDTO;
import com.zhikao.learning.domain.dto.LearningRecordFormDTO;
import com.zhikao.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface ILearningRecordService extends IService<LearningRecord> {

    LearningLessonDTO queryLearningRecordByCourse(Long courseId);

    void addLearningRecord(LearningRecordFormDTO formDTO);

    /**
     * 统计当前用户在指定课程的累计学时（分钟）
     *
     * @param courseId 课程id
     * @return 累计学时
     */
    Integer queryStudyMinutes(Long courseId);
}
