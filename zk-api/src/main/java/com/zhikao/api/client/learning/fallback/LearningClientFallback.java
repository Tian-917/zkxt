package com.zhikao.api.client.learning.fallback;

import com.zhikao.api.client.learning.LearningClient;
import com.zhikao.api.dto.leanring.LearningLessonDTO;
import com.zhikao.api.dto.leanring.LearningRecordFormDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class LearningClientFallback implements FallbackFactory<LearningClient> {

    @Override
    public LearningClient create(Throwable cause) {
        log.error("查询学习服务异常", cause);
        return new LearningClient() {
            @Override
            public Integer countLearningLessonByCourse(Long courseId) {
                return 0;
            }

            @Override
            public Long isLessonValid(Long courseId) {
                return null;
            }

            @Override
            public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
                return null;
            }

            @Override
            public void addLearningRecord(LearningRecordFormDTO formDTO) {
                log.error("提交学习记录失败，lessonId={}, sectionId={}", formDTO.getLessonId(), formDTO.getSectionId());
            }

            @Override
            public Integer queryStudyMinutes(Long courseId) {
                log.error("查询学时失败，courseId={}", courseId);
                return 0;
            }
        };
    }
}
