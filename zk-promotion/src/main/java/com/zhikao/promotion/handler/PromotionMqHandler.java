package com.zhikao.promotion.handler;

import com.zhikao.common.constants.MqConstants;
import com.zhikao.common.utils.UserContext;
import com.zhikao.promotion.domain.dto.UserCouponDTO;
import com.zhikao.promotion.domain.po.Coupon;
import com.zhikao.promotion.domain.po.ExchangeCode;
import com.zhikao.promotion.enums.ExchangeCodeStatus;
import com.zhikao.promotion.service.IExchangeCodeService;
import com.zhikao.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PromotionMqHandler {

    private final IUserCouponService userCouponService;
    private final IExchangeCodeService exchangeCodeService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "coupon.receive.queue",durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.PROMOTION_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.COUPON_RECEIVE
    ))
    public void listenCouponReceiveMessage(UserCouponDTO userCouponDTO){
        userCouponService.checkAndCreateUserCoupon(userCouponDTO);
    }


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "coupon.exchange.queue",durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.PROMOTION_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.COUPON_EXCHANGE
    ))
    @Transactional
    public void listenCouponExchangeMessage(UserCouponDTO userCouponDTO){
        // 1.校验并生成用户券
        userCouponService.checkAndCreateUserCoupon(userCouponDTO);
        // 2.标记兑换码为已兑换
        exchangeCodeService.lambdaUpdate()
                .set(ExchangeCode::getUserId, userCouponDTO.getUserId())
                .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                .eq(ExchangeCode::getId, userCouponDTO.getCodeId())
                .update();
    }


}
