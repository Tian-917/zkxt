package com.zhikao.learning.controller;


import com.zhikao.api.dto.leanring.LearningLessonDTO;
import com.zhikao.learning.domain.dto.LearningRecordFormDTO;
import com.zhikao.learning.service.ILearningRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/learning-records")
public class LearningRecordController {

    private final ILearningRecordService recordService;

    /**
     * 查询当前用户指定课程的学习进度
     * @param courseId 课程id
     * @return 课表信息、学习记录及进度信息
     */
    @GetMapping("/course/{courseId}")
    public LearningLessonDTO queryLearningRecordByCourse(@PathVariable("courseId") Long courseId){
        return recordService.queryLearningRecordByCourse(courseId);
    }

    /**
     * 提交学习记录
     * @param formDTO
     */
    @PostMapping
    public void addLearningRecord(@RequestBody LearningRecordFormDTO formDTO){
        recordService.addLearningRecord(formDTO);
    }

    /**
     * 统计当前用户在指定课程的累计学时（分钟）
     * <p>证书域发证资格判定用</p>
     *
     * @param courseId 课程id
     * @return 累计学时（分钟）
     */
    @GetMapping("/course/{courseId}/study-minutes")
    public Integer queryStudyMinutes(@PathVariable("courseId") Long courseId) {
        return recordService.queryStudyMinutes(courseId);
    }
}
