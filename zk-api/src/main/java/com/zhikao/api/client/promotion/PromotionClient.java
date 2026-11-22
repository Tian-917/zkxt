package com.zhikao.api.client.promotion;

import com.zhikao.api.client.promotion.fallback.PromotionClientFallback;
import com.zhikao.api.dto.promotion.CouponDiscountDTO;
import com.zhikao.api.dto.promotion.OrderCouponDTO;
import com.zhikao.api.dto.promotion.OrderCourseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(value = "promotion-service", fallbackFactory =  PromotionClientFallback.class)
public interface PromotionClient {

    /**
     * 查询我的优惠券可用方案
     * @param orderCourses
     * @return
     */
    @PostMapping("/user-coupons/available")
    List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> orderCourses);

    /**
     * 根据优惠券方案计算订单优惠明细
     * @param orderCouponDTO
     * @return 优惠券方案明细
     */
    @PostMapping("/user-coupons/discount")
    CouponDiscountDTO queryDiscountSolutionDetail(@RequestBody OrderCouponDTO orderCouponDTO);

    /**
     * 核销指定优惠券
     * @param couponIds 用户优惠券id集合
     */
    @PutMapping("/user-coupons/use")
    void writeOffCoupon(@RequestParam("couponIds") List<Long> couponIds);

    /**
     * 退还指定优惠券
     * @param couponIds 用户优惠券id集合
     */
    @PutMapping("/user-coupons/refund")
    void refundCoupon(@RequestParam("couponIds") List<Long> couponIds);

    /**
     * 查询优惠券规则
     * @param couponIds 用户优惠券id集合
     */
    @GetMapping("/user-coupons/rules")
    List<String> queryDiscountRules(@RequestParam("couponIds") List<Long> couponIds);
}
