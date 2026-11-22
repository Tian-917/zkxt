package com.zhikao.trade.mapper;

import com.zhikao.trade.domain.po.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 订单 Mapper 接口
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface OrderMapper extends BaseMapper<Order> {

    Order getById(Long id);
}
