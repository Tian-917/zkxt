package com.zhikao.promotion.controller;


import com.zhikao.api.dto.promotion.CouponDiscountDTO;
import com.zhikao.api.dto.promotion.OrderCouponDTO;
import com.zhikao.api.dto.promotion.OrderCourseDTO;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.common.exceptions.BadRequestException;
import com.zhikao.promotion.domain.vo.CouponVO;
import com.zhikao.promotion.enums.UserCouponStatus;
import com.zhikao.promotion.service.IDiscountService;
import com.zhikao.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/user-coupons")
@RequiredArgsConstructor
public class UserCouponController {

    private final IUserCouponService userCouponService;
    private final IDiscountService discountService;


    /**
     * 手动领取优惠券接口
     * @param couponId
     */
    @PostMapping("/{couponId}/receive")
    public void receiveCoupon(@PathVariable("couponId") Long couponId) {
        userCouponService.receiveCoupon(couponId);
    }

    /**
     * 兑换码兑换优惠券接口
     * @param code
     */
    @PostMapping("/{code}/exchange")
    public void exchangeCoupon(@PathVariable("code") String code) {
        userCouponService.exchangeCoupon(code);
    }

    /**
     * 查询我的优惠券可用方案
     * @param orderCourses
     * @return
     */
    @PostMapping("/available")
    public List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> orderCourses) {
        return discountService.findDiscountSolution(orderCourses);
    }

    /**
     * 根据优惠券方案计算订单优惠明细
     * @param orderCouponDTO
     * @return
     */
    @PostMapping("/discount")
    public CouponDiscountDTO queryDiscountSolutionDetail(@RequestBody OrderCouponDTO  orderCouponDTO) {
        return discountService.queryDiscountSolutionDetail(orderCouponDTO);
    }

    /**
     * 分页查询我的优惠券
     * @param pageQuery
     * @param status
     * @return
     */
    @GetMapping("/page")
    public PageDTO<CouponVO> queryMyCouponPage(PageQuery pageQuery,
                                                @RequestParam("status") Integer status) {
        UserCouponStatus userCouponStatus = UserCouponStatus.of(status);
        if (userCouponStatus == null) {
            throw new BadRequestException("优惠券状态不正确");
        }
        return userCouponService.queryMyCouponPage(pageQuery, userCouponStatus);
    }

    /**
     * 退还指定优惠券
     * @param couponIds
     */
    @PutMapping("/refund")
    public void refundCoupon(@RequestParam("couponIds") List<Long> couponIds) {
        userCouponService.refundCoupon(couponIds);
    }

    /**
     * 查询优惠券规则
     * @param couponIds
     * @return
     */
    @GetMapping("/rules")
    public List<String> queryDiscountRules(@RequestParam("couponIds") List<Long> couponIds){
        return userCouponService.queryDiscountRules(couponIds);
    }

    /**
     * 核销指定优惠券
     * @param couponIds
     */
    @PutMapping("/use")
    public void useCoupon(@RequestParam("couponIds") List<Long> couponIds) {
        userCouponService.useCoupon(couponIds);
    }
}
