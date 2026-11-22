package com.zhikao.learning.controller;


import com.zhikao.common.utils.BeanUtils;
import com.zhikao.common.utils.CollUtils;
import com.zhikao.learning.domain.po.PointsBoardSeason;
import com.zhikao.learning.domain.query.PointsBoardQuery;
import com.zhikao.learning.domain.vo.PointsBoardSeasonVO;
import com.zhikao.learning.domain.vo.PointsBoardVO;
import com.zhikao.learning.service.IPointsBoardSeasonService;
import com.zhikao.learning.service.IPointsBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class PointsBoardController {

    private final IPointsBoardService pointsBoardService;

    private final IPointsBoardSeasonService seasonService;

    /**
     * 分页查询指定赛季的积分排行榜
     * @param query
     * @return
     */
    @GetMapping
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query){
        return pointsBoardService.queryPointsBoardBySeason(query);
    }

    /**
     * 查询赛季信息列表，只返回已经开始过的赛季
     * @return
     */
    @GetMapping("/seasons/list")
    public List<PointsBoardSeasonVO> queryPointsBoardSeasons(){
        // 1.获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 2.查询开始时间不晚于当前时间的赛季
        List<PointsBoardSeason> list = seasonService.lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, now)
                .list();
        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyList();
        }
        // 3.转换为VO返回
        return BeanUtils.copyToList(list, PointsBoardSeasonVO.class);
    }
}
