package com.zhikao.learning.service;

import com.zhikao.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.learning.domain.query.PointsBoardQuery;
import com.zhikao.learning.domain.vo.PointsBoardVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IPointsBoardService extends IService<PointsBoard> {

    PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query);

    void createPointsBoardTableBySeason(Integer season);

    List<PointsBoard> queryCurrentBoardList(String key, Integer pageNo, Integer pageSize);
}
