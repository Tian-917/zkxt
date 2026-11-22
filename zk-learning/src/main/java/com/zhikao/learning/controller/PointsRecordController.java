package com.zhikao.learning.controller;


import com.zhikao.learning.domain.po.PointsRecord;
import com.zhikao.learning.domain.vo.PointsStatisticsVO;
import com.zhikao.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsRecordController {

    private final IPointsRecordService recordService;

    /**
     * 查询我的今日所有积分情况
     * @return
     */
    @GetMapping("/today")
    public List<PointsStatisticsVO> queryMyPointsToday(){
        return recordService.queryMyPointsToday();
    }

}
