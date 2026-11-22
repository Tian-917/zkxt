package com.zhikao.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.pay.sdk.dto.RefundResultDTO;
import com.zhikao.trade.domain.dto.ApproveFormDTO;
import com.zhikao.trade.domain.dto.RefundCancelDTO;
import com.zhikao.trade.domain.dto.RefundFormDTO;
import com.zhikao.trade.domain.po.RefundApply;
import com.zhikao.trade.domain.query.RefundApplyPageQuery;
import com.zhikao.trade.domain.vo.RefundApplyPageVO;
import com.zhikao.trade.domain.vo.RefundApplyVO;

import java.util.List;

/**
 * <p>
 * 退款申请 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IRefundApplyService extends IService<RefundApply> {

    List<RefundApply> queryByDetailId(Long id);

    void applyRefund(RefundFormDTO refundFormDTO);

    PageDTO<RefundApplyPageVO> queryRefundApplyByPage(RefundApplyPageQuery pageQuery);

    RefundApplyVO queryRefundDetailById(Long id);

    RefundApplyVO nextRefundApplyToApprove();

    void approveRefundApply(ApproveFormDTO approveDTO);

    void cancelRefundApply(RefundCancelDTO cancelDTO);

    RefundApplyVO queryRefundDetailByDetailId(Long id);

    void handleRefundResult(RefundResultDTO refundResult);

    List<RefundApply> queryApplyToSend(int page, int size);

    void sendRefundRequest(RefundApply refundApply);

    boolean checkRefundStatus(RefundApply refundApply);
}
