package com.zhikao.pay.service;

import com.zhikao.pay.sdk.dto.PayChannelDTO;
import com.zhikao.pay.domain.po.PayChannel;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 支付渠道 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IPayChannelService extends IService<PayChannel> {

    Long addPayChannel(PayChannelDTO channelDTO);

    void updatePayChannel(PayChannelDTO channelDTO);
}
