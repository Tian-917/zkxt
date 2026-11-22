package com.zhikao.promotion.handler;

import com.zhikao.promotion.service.IExchangeCodeService;
import com.zhikao.promotion.service.IUserCouponService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponExpireJobHandler {

    private final IUserCouponService userCouponService;
    private final IExchangeCodeService exchangeCodeService;

    @XxlJob("expireCouponJobHandler")
    public void expireCoupon() {
        // 1.过期的用户优惠券置为已失效
        userCouponService.expireCoupon();
        // 2.过期的兑换码置为已失效
        exchangeCodeService.expireCode();
    }
}
