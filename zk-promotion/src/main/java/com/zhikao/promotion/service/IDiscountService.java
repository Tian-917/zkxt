package com.zhikao.promotion.service;

import com.zhikao.api.dto.promotion.CouponDiscountDTO;
import com.zhikao.api.dto.promotion.OrderCouponDTO;
import com.zhikao.api.dto.promotion.OrderCourseDTO;

import java.util.List;

public interface IDiscountService {
    List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses);

    CouponDiscountDTO queryDiscountSolutionDetail(OrderCouponDTO  orderCouponDTO);
}
