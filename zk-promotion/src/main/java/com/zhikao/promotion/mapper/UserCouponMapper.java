package com.zhikao.promotion.mapper;

import com.zhikao.promotion.domain.po.Coupon;
import com.zhikao.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 Mapper 接口
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    List<Coupon> queryMyCoupons(@Param("userId") Long user);
}
