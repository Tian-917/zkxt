package com.zhikao.promotion.handler;

import com.zhikao.promotion.service.ICouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 优惠券发放状态定时任务：
 * 1. 把到达发放开始时间的待发放券切换为发放中，并写入领取缓存
 * 2. 把超过发放结束时间的券切换为已结束
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueJobHandler {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ICouponService couponService;

    @XxlJob("couponIssueJobHandler")
    public void handleCouponIssueJob() {
        // 1.分片索引作为页码（从1开始），分片参数作为每页大小
        int page = XxlJobHelper.getShardIndex() + 1;
        int size = parsePageSize(XxlJobHelper.getJobParam());
        // 2.处理到点待发放的券
        couponService.issueCouponByPage(page, size);
        // 3.处理已过发放结束时间的券
        couponService.finishIssuingCoupons();
    }

    private int parsePageSize(String param) {
        try {
            int size = Integer.parseInt(param);
            return size > 0 ? size : DEFAULT_PAGE_SIZE;
        } catch (NumberFormatException e) {
            return DEFAULT_PAGE_SIZE;
        }
    }
}
