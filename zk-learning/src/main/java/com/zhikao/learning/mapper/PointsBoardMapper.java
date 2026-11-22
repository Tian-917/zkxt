package com.zhikao.learning.mapper;

import com.zhikao.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 学霸天梯榜 Mapper 接口
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface PointsBoardMapper extends BaseMapper<PointsBoard> {


    void createPointsBoardTable(@Param("tableName") String tableName);
}
