package com.zhikao.data.model.vo;

import lombok.Data;

import java.util.List;

/**
 * echartVO
 * @ClassName EchartsVO
 * @Author Tian
 * @Date 2026-11-19
 * @Version
 **/
@Data
public class EchartsVO {
    private List<AxisVO> xAxis;
    private List<AxisVO> yAxis;
    private List<SerierVO> series;
}
