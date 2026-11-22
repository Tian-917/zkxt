package com.zhikao.api.client.course.fallback;

import com.zhikao.api.client.course.CourseClient;
import com.zhikao.api.dto.course.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

@Slf4j
public class CourseClientFallback implements FallbackFactory<CourseClient> {
    @Override
    public CourseClient create(Throwable cause) {
        log.error("查询课程服务出现异常", cause);
        return new CourseClient() {
            @Override
            public List<SubNumAndCourseNumDTO> infoByTeacherIds(Iterable<Long> teacherIds) {
                return Collections.emptyList();
            }

            @Override
            public SectionInfoDTO sectionInfo(Long sectionId) {
                return null;
            }

            @Override
            public List<MediaQuoteDTO> mediaUserInfo(Iterable<Long> mediaIds) {
                return Collections.emptyList();
            }

            @Override
            public CourseSearchDTO getSearchInfo(Long id) {
                return null;
            }

            @Override
            public List<CourseSimpleInfoDTO> getSimpleInfoList(Iterable<Long> ids) {
                return Collections.emptyList();
            }

            @Override
            public CourseFullInfoDTO getCourseInfoById(Long id, boolean withCatalogue, boolean withTeachers) {
                return null;
            }
        };
    }
}
