package com.zhikao.learning.mq;


import com.zhikao.api.dto.trade.OrderBasicDTO;
import com.zhikao.common.constants.MqConstants;
import com.zhikao.common.utils.CollUtils;
import com.zhikao.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonChangeListener {

    private final ILearningLessonService LessonService;

    /**
     * 支付或报名课程后，立刻加入课表
     * @param order
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.pay.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void listenLessonPay(OrderBasicDTO order) {
        // 1.健壮性处理
        if(order == null || order.getOrderId() == null || CollUtils.isEmpty(order.getCourseIds())){
            // 数据有误，无需处理
            log.error("接收到MQ消息有误，订单数据为空");
            return;
        }
        // 2.添加课程
        log.debug("监听到用户{}的订单{},需要添加课程{}到课表中", order.getUserId(),order.getOrderId(),order.getCourseIds());
        LessonService.addUserLessons(order.getUserId(), order.getCourseIds());
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.refund.queue", declare = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_REFUND_KEY
    ))
    public void listenLessonRefund(OrderBasicDTO order) {
        // 1.健壮性处理
        if(order == null || order.getOrderId() == null || CollUtils.isEmpty(order.getCourseIds())){
            // 数据有误，无需处理
            log.error("接收到MQ消息有误，订单数据为空");
            return;
        }
        // 2.从课表中移除课程
        for (Long courseId : order.getCourseIds()) {
            LessonService.deleteLessonByUserIdAndCourseId(order.getUserId(), courseId);
        }
    }
}
