package com.zhikao.cert.mq;

import com.zhikao.cert.constants.CertConstants;
import com.zhikao.common.constants.MqConstants;
import com.zhikao.api.dto.cert.CertIssueDTO;
import com.zhikao.cert.service.ICertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 考核通过事件监听：考试服务交卷判分后发出，证书域异步发证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertIssueListener {

    private final ICertificateService certificateService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = CertConstants.CERT_ISSUE_QUEUE, durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.CERT_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.CERT_ISSUE_KEY
    ))
    public void listenExamPassed(CertIssueDTO dto) {
        log.info("监听到考核通过事件：userId={}, examId={}", dto.getUserId(), dto.getExamId());
        certificateService.handleExamPassed(dto);
    }
}
