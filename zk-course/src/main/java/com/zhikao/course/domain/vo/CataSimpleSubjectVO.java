package com.zhikao.course.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Tian
 * @since 2026-11-19
 * @version 1.0.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CataSimpleSubjectVO {
    @ApiModelProperty("小节或练习id")
    private Long cataId;
    @ApiModelProperty("题目id")
    private List<SubjectInfo> subjects;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubjectInfo{
        private Long id;
        private String name;
    }
}
