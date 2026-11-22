package com.zhikao.learning.mq;


import com.zhikao.common.constants.MqConstants;
import com.zhikao.learning.enums.PointsRecordType;
import com.zhikao.learning.mq.message.SignInMessage;
import com.zhikao.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LearningPoints {

    private final IPointsRecordService recordService;

    /**
     * 签到
     * @param message
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "sign.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.SIGN_IN
    ))
    public void listenSignInMessage(SignInMessage message){
        recordService.addPointsRecord(message.getUserId(), message.getPoint(), PointsRecordType.SIGN);
    }

    /**
     * 学习小节
     * @param userId
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "learning.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.LEARN_SECTION
    ))
    public void listenLearnSectionMessage(Long userId){
        recordService.addPointsRecord(userId, 10, PointsRecordType.LEARNING);
    }

    /**
     * 写问答
     * @param userId
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_REPLY
    ))
    public void listenWriteReplyMessage(Long userId){
        recordService.addPointsRecord(userId, 5, PointsRecordType.QA);
    }

    /**
     * 写笔记
     * @param userId
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "note.new.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_NOTE
    ))
    public void listenWriteNoteMessage(Long userId){
        recordService.addPointsRecord(userId, 3, PointsRecordType.NOTE);
    }

    /**
     * 笔记被采集
     * @param userId
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "note.gathered.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.NOTE_GATHERED
    ))
    public void listenNoteGatheredMessage(Long userId){
        recordService.addPointsRecord(userId, 2, PointsRecordType.NOTE);
    }

}
