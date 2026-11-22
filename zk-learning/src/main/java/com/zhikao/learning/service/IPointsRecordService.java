package com.zhikao.learning.service;

import com.zhikao.learning.domain.po.PointsRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.learning.domain.vo.PointsStatisticsVO;
import com.zhikao.learning.enums.PointsRecordType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IPointsRecordService extends IService<PointsRecord> {

    void addPointsRecord(Long userId, int point, PointsRecordType type);

    List<PointsStatisticsVO> queryMyPointsToday();

    int queryUserPointsByTypeAndDate(Long userId, PointsRecordType type, LocalDateTime begin, LocalDateTime end);
}
