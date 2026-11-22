package com.zhikao.promotion.service;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.promotion.domain.po.Coupon;
import com.zhikao.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.promotion.domain.vo.ExchangeCodeVO;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {

    void asyncGenerateCode(Coupon coupon);

    boolean updateExchangeMark(long serialNum, boolean b);

    PageDTO<ExchangeCodeVO> queryCodePage(PageQuery pageQuery, Integer status, Long couponId);

    void expireCode();
}
