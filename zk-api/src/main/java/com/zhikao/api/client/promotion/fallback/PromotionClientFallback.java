package com.zhikao.api.client.promotion.fallback;

import com.zhikao.api.client.promotion.PromotionClient;
import com.zhikao.api.dto.promotion.CouponDiscountDTO;
import com.zhikao.api.dto.promotion.OrderCouponDTO;
import com.zhikao.api.dto.promotion.OrderCourseDTO;
import com.zhikao.common.exceptions.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

@Slf4j
public class PromotionClientFallback implements FallbackFactory<PromotionClient> {
    @Override
    public PromotionClient create(Throwable cause) {
        log.error("查询促销服务出现异常，",cause);
        return new PromotionClient() {
            @Override
            public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses) {
                return Collections.emptyList();
            }

            @Override
            public CouponDiscountDTO queryDiscountSolutionDetail(OrderCouponDTO orderCouponDTO) {
                // 促销服务不可用时按无优惠降级
                return new CouponDiscountDTO();
            }

            @Override
            public void writeOffCoupon(List<Long> couponIds) {
                log.error("核销优惠券失败，couponIds={}", couponIds);
                // 核销失败必须抛出异常，触发订单事务回滚，避免订单有优惠但券未核销
                throw new BizIllegalException(500, "核销优惠券异常", cause);
            }

            @Override
            public void refundCoupon(List<Long> couponIds) {
                log.error("退还优惠券失败，couponIds={}", couponIds);
                // 退券失败必须抛出异常，避免订单关闭但优惠券没有退回
                throw new BizIllegalException(500, "退还优惠券异常", cause);
            }

            @Override
            public List<String> queryDiscountRules(List<Long> couponIds) {
                // 查询优惠券规则失败时返回空列表，不影响订单详情主流程
                return Collections.emptyList();
            }
        };
    }
}
