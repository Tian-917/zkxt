package com.zhikao.promotion.controller;


import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.promotion.domain.vo.ExchangeCodeVO;
import com.zhikao.promotion.service.IExchangeCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 兑换码 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/codes")
@RequiredArgsConstructor
public class ExchangeCodeController {

    private final IExchangeCodeService exchangeCodeService;

    @GetMapping("/page")
    public PageDTO<ExchangeCodeVO> queryCodePage(PageQuery pageQuery,
                                                 @RequestParam("status") Integer status,
                                                 @RequestParam("couponId") Long couponId) {
        return exchangeCodeService.queryCodePage(pageQuery,status,couponId);
    }
}
