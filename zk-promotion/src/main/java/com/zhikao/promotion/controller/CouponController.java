package com.zhikao.promotion.controller;


import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.promotion.domain.dto.CouponFormDTO;
import com.zhikao.promotion.domain.dto.CouponIssueFormDTO;
import com.zhikao.promotion.domain.query.CouponQuery;
import com.zhikao.promotion.domain.vo.CouponDetailVO;
import com.zhikao.promotion.domain.vo.CouponPageVO;
import com.zhikao.promotion.domain.vo.CouponVO;
import com.zhikao.promotion.service.ICouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final ICouponService couponService;

    /**
     * 新增优惠券接口
     * @param dto
     */
    @PostMapping
    public void saveCoupon(@RequestBody @Valid CouponFormDTO dto) {
        couponService.saveCoupon(dto);
    }

    /**
     * 分页查询优惠券接口
     * @param query
     * @return
     */
    @GetMapping("/page")
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query) {
        return couponService.queryCouponByPage(query);
    }

    /**
     * 根据id查询优惠券
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public CouponDetailVO queryCouponById(@PathVariable Long id) {
        return couponService.queryCouponById(id);
    }

    /**
     * 根据id删除优惠券
     * @param id
     */
    @DeleteMapping("/{id}")
    public void deleteCouponById(@PathVariable Long id) {
        couponService.deleteCouponById(id);
    }


    /**
     * 根据id更新优惠券
     * @param id 优惠券id
     * @param dto 优惠券表单数据
     */
    @PutMapping("/{id}")
    public void updateCouponById(@PathVariable Long id, @Valid @RequestBody CouponFormDTO dto) {
        dto.setId(id);
        couponService.updateCouponById(dto);
    }

    /**
     * 发放优惠券接口
     * @param dto
     */
    @PutMapping("/{id}/issue")
    public void beginIssue(@PathVariable Long id, @RequestBody @Valid CouponIssueFormDTO dto) {
        dto.setId(id);
        couponService.beginIssue(dto);
    }

    /**
     * 查询发放优惠券列表
     * @return
     */
    @GetMapping("/list")
    public List<CouponVO> queryIssuingCoupons() {
        return couponService.queryIssuingCoupons();
    }

    /**
     * 暂停发放优惠券
     * @param id
     */
    @PutMapping("/{id}/pause")
    public void pauseIssue(@PathVariable Long id) {
        couponService.pauseIssue(id);
    }

}


