package com.zhikao.pay.service;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.pay.domain.po.RefundOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.pay.sdk.dto.RefundApplyDTO;
import com.zhikao.pay.sdk.dto.RefundResultDTO;

/**
 * <p>
 * 退款订单 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IRefundOrderService extends IService<RefundOrder> {

    RefundResultDTO applyRefund(RefundApplyDTO refundApplyDTO);

    RefundResultDTO queryRefundResult(Long bizRefundOrderId);

    RefundOrder queryByRefundOrderNo(Long refundOrderNo);

    PageDTO<RefundOrder> queryRefundingOrderByPage(int pageNo, int size);

    void checkRefundOrder(RefundOrder refundOrder);

    RefundResultDTO queryRefundDetail(Long bizRefundOrderId);
}
