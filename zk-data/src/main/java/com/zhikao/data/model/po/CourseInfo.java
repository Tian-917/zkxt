package com.zhikao.data.model.po;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName CourseInfo
 * @Author Tian
 * @Date 2026-11-19
 * @Version
 **/
@Data
public class CourseInfo implements Serializable {
    private String category;
    private String name;
    private Integer newStuNum;
    private Double orderAmount;
}
