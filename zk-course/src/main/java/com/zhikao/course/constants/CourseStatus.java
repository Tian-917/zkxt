package com.zhikao.course.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @ClassName CourseStatus
 * @Author Tian
 * @Date 2026-11-19
 * @Version
 **/
@Getter
@AllArgsConstructor
public enum CourseStatus {
    NO_UP_SHELF(1, "待上架"),
    SHELF(2, "已上架"),
    DOWN_SHELF(3, "下架"),
    FINISHED(4, "已完结");
    private Integer status;
    private String desc;

    public static String desc(Integer status) {
        for (CourseStatus courseStatus : values()) {
            if (courseStatus.getStatus() == status) {
                return courseStatus.getDesc();
            }
        }
        return null;
    }

    public boolean equals(Integer status){
        // status 为 null 时直接返回 false：原来直接拆箱会 NPE（管理端课程列表不带 status 就崩）
        return status != null && this.status.intValue() == status.intValue();
    }
}