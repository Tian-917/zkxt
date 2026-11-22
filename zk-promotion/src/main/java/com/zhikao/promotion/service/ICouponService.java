package com.zhikao.promotion.service;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.promotion.domain.dto.CouponFormDTO;
import com.zhikao.promotion.domain.dto.CouponIssueFormDTO;
import com.zhikao.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.promotion.domain.query.CouponQuery;
import com.zhikao.promotion.domain.vo.CouponDetailVO;
import com.zhikao.promotion.domain.vo.CouponPageVO;
import com.zhikao.promotion.domain.vo.CouponVO;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface ICouponService extends IService<Coupon> {

    void saveCoupon(@Valid CouponFormDTO dto);

    PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query);

    CouponDetailVO queryCouponById(Long id);

    void deleteCouponById(Long id);

    void updateCouponById(CouponFormDTO dto);

    void beginIssue(@Valid CouponIssueFormDTO dto);

    void beginIssueBatch(List<Coupon> coupons);

    void issueCouponByPage(int page, int size);

    void finishIssuingCoupons();

    List<CouponVO> queryIssuingCoupons();

    void pauseIssue(Long id);
}
