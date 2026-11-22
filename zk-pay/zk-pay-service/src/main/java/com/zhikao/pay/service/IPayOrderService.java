package com.zhikao.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.pay.domain.po.PayOrder;
import com.zhikao.pay.sdk.dto.PayApplyDTO;
import com.zhikao.pay.sdk.dto.PayResultDTO;

import java.time.LocalDateTime;

/**
 * <p>
 * 支付订单 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IPayOrderService extends IService<PayOrder> {

    String applyPayOrder(PayApplyDTO payApplyDTO);

    PayOrder queryByBizOrderNo(Long bizOrderNo);
    PayResultDTO queryPayResult(Long bizOrderId);

    PayOrder queryByPayOrderNo(Long payOrderNo);

    boolean markPayOrderSuccess(Long id, LocalDateTime successTime);

    PageDTO<PayOrder> queryPayingOrderByPage(int page, int size);

    void checkPayOrder(PayOrder payOrder);

    /**
     * 模拟支付成功（仅 mock 渠道使用）
     * <p>把支付单置为成功并向业务端发 PAY_SUCCESS 消息，等价于第三方回调。</p>
     *
     * @param bizOrderNo 业务订单号
     * @return 是否本次真正完成了状态流转（false 表示幂等命中或状态已变更）
     */
    boolean mockPaySuccess(Long bizOrderNo);
}
