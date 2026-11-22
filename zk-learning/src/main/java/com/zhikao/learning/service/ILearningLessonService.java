package com.zhikao.learning.service;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.learning.domain.dto.LearningPlanDTO;
import com.zhikao.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.learning.domain.vo.LearningLessonVO;
import com.zhikao.learning.domain.vo.LearningPlanPageVO;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    void addUserLessons(Long userId, List<Long> courseIds);

    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    LearningLessonVO queryNowLesson();

    LearningLessonVO queryLessonById(Long courseId);

    void deleteLessonById(Long courseId);

    void deleteLessonByUserIdAndCourseId(Long userId, Long courseId);

    Long isLessonValid(Long courseId);

    Integer countLearningLessonByCourse(Long courseId);

    LearningLesson queryByUserIdAndCourseId(Long userId, Long courseId);

    void createLearningPlan(@Valid LearningPlanDTO planDTO);

    LearningPlanPageVO queryMyPlans(PageQuery query);
}
