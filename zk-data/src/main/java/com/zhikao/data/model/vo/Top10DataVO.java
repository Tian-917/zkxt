package com.zhikao.data.model.vo;

import com.zhikao.data.model.po.CourseInfo;
import lombok.Data;

import java.util.List;

/**
 * @ClassName Top10DataVO
 * @Author Tian
 * @Date 2026-11-19
 * @Version
 **/
@Data
public class Top10DataVO {
    // 热门课程
    private List<CourseInfo> hot;
    // 热销课程
    private List<CourseInfo> hotSales;
}
