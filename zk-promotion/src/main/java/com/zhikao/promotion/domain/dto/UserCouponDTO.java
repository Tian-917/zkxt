package com.zhikao.promotion.domain.dto;

import lombok.Data;

@Data
public class UserCouponDTO {
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 优惠券id
     */
    private Long couponId;

    /**
     * 兑换码id（兑换码序列号），仅兑换码兑换场景需要传递
     */
    private Long codeId;
}
