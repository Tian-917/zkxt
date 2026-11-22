package com.zhikao.trade.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单取消原因
 */
@Getter
@AllArgsConstructor
public enum OrderCancelReason {

    USER_CANCEL("用户取消订单"),
    TIME_OUT("支付超时，订单自动取消");

    private final String message;
}
