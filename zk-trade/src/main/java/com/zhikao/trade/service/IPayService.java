package com.zhikao.trade.service;

import com.zhikao.trade.domain.dto.OrderDelayQueryDTO;
import com.zhikao.trade.domain.dto.PayApplyFormDTO;
import com.zhikao.trade.domain.vo.PayChannelVO;

import java.util.List;

public interface IPayService {
    List<PayChannelVO> queryPayChannels();

    String applyPayOrder(PayApplyFormDTO payApply);

    void queryPayResult(OrderDelayQueryDTO message);
}
