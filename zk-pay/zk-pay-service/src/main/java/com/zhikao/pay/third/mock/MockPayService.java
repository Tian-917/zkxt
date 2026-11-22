package com.zhikao.pay.third.mock;

import com.zhikao.pay.third.IPayService;
import com.zhikao.pay.third.model.PayStatus;
import com.zhikao.pay.third.model.PayStatusResponse;
import com.zhikao.pay.third.model.PrepayResponse;
import com.zhikao.pay.third.model.RefundResponse;
import com.zhikao.pay.third.model.RefundStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 本地模拟支付渠道。
 * <p>
 * bean 名 {@code mock} 就是渠道 code（{@code PayOrderServiceImpl} 用
 * {@code Map<String, IPayService>} 按 bean 名选渠道），所以不需要改任何工厂代码。
 * </p>
 * <p>
 * 用途：个人开发者拿不到微信/支付宝商户号，用它把
 * 「下单 → 支付单 → 支付成功 → 回调业务 → 订单完成」整条链路在本地跑通：
 * 创建支付单会返回 {@code mock://pay?...}，随后调用
 * {@code POST /ps/pay-orders/{bizOrderNo}/mock-pay} 即视为支付成功。
 * </p>
 */
@Slf4j
@Component("mock")
public class MockPayService implements IPayService {

    public static final String CHANNEL_CODE = "mock";

    @Override
    public PrepayResponse createPrepayOrder(String title, String orderNo, Integer amount) {
        log.info("[模拟支付] 创建支付单 title={}, payOrderNo={}, amount={}分", title, orderNo, amount);
        return PrepayResponse.builder()
                .success(true)
                .payUrl("mock://pay?orderNo=" + orderNo + "&amount=" + amount)
                .build();
    }

    @Override
    public PayStatusResponse queryPayOrderStatus(String payOrderNo) {
        // 模拟渠道不主动查单：支付结果由 /pay-orders/{bizOrderNo}/mock-pay 显式触发
        return PayStatusResponse.builder()
                .success(true)
                .payOrderNo(payOrderNo)
                .payStatus(PayStatus.WAIT_BUYER_PAY.getValue())
                .build();
    }

    @Override
    public RefundResponse refundOrder(String payOrderNo, String refundOrderNo,
                                      Integer refundAmount, Integer totalAmount) {
        log.info("[模拟支付] 退款 payOrderNo={}, refundOrderNo={}, amount={}分", payOrderNo, refundOrderNo, refundAmount);
        return RefundResponse.builder()
                .success(true)
                .channel(CHANNEL_CODE)
                .amount(refundAmount)
                .status(RefundStatus.SUCCESS.getValue())
                .msg("模拟退款成功")
                .build();
    }

    @Override
    public RefundResponse queryRefundStatus(String orderNo, String refundOrderNo) {
        return RefundResponse.builder()
                .success(true)
                .channel(CHANNEL_CODE)
                .status(RefundStatus.SUCCESS.getValue())
                .build();
    }
}
