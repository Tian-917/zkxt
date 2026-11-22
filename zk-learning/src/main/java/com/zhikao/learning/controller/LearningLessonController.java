package com.zhikao.learning.controller;


import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.learning.domain.dto.LearningPlanDTO;
import com.zhikao.learning.domain.vo.LearningLessonVO;
import com.zhikao.learning.domain.vo.LearningPlanPageVO;
import com.zhikao.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LearningLessonController {

    private final ILearningLessonService lessonService;

    /**
     * 分页查询我的课表
     * @param query
     * @return
     */
    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        return lessonService.queryMyLessons(query);
    }

    /**
     * 查询最近正在学习的课程
     * @return
     */
    @GetMapping("/now")
    public LearningLessonVO queryNowLesson() {return lessonService.queryNowLesson();}

    /**
     * 根据id查询指定课程的学习状态
     * @param courseId
     * @return
     */
    @GetMapping("/{courseId}")
    public LearningLessonVO queryLessonById(@PathVariable("courseId") Long courseId) {
        return lessonService.queryLessonById(courseId);
    }

    /**
     * 根据id删除课表中的某课程
     * @param courseId
     */
    @DeleteMapping("/{courseId}")
    public void deleteLessonById(@PathVariable("courseId") Long courseId) {
         lessonService.deleteLessonById(courseId);
    }

    /**
     * 校验指定课程是否是课程表中的有效课程
     * @param courseId
     */
    @GetMapping("/{courseId}/valid")
    public Long isLessonValid(@PathVariable("courseId") Long courseId) {
        return lessonService.isLessonValid(courseId);
    }

    /**
     * 统计课程学习人数
     * @param courseId
     * @return
     */
    @GetMapping("/{courseId}/count")
    public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId) {
        return lessonService.countLearningLessonByCourse(courseId);
    }

    /**
     * 创建学习计划
     * @param planDTO
     */
    @PostMapping("/plans")
    public void createLearnPlans(@Valid @RequestBody LearningPlanDTO planDTO) {
        lessonService.createLearningPlan(planDTO);
    }

    /**
     * 查询我的学习计划
     * @param query
     * @return
     */
    @GetMapping("/plans")
    public LearningPlanPageVO queryMyPlans(PageQuery query) {
        return lessonService.queryMyPlans(query);
    }
}
