package com.zhikao.cert.controller;

import com.zhikao.common.constants.MqConstants;
import com.zhikao.api.dto.cert.CertIssueDTO;
import com.zhikao.cert.domain.vo.CertificateVO;
import com.zhikao.cert.domain.vo.VerifyResultVO;
import com.zhikao.cert.service.ICertificateService;
import com.zhikao.common.autoconfigure.mq.RabbitMqHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 证书接口
 */
@Api(tags = "证书相关接口")
@RestController
@RequestMapping("/cert")
@RequiredArgsConstructor
public class CertificateController {

    private final ICertificateService certificateService;
    private final RabbitMqHelper rabbitMqHelper;

    @ApiOperation("发证（同步，内部/管理端调用）")
    @PostMapping("/issue")
    public CertificateVO issue(@Valid @RequestBody CertIssueDTO dto) {
        return certificateService.issue(dto);
    }

    @ApiOperation("模拟考试服务发布「考核通过」事件（异步发证链路演示）")
    @PostMapping("/exam-passed")
    public void examPassed(@Valid @RequestBody CertIssueDTO dto) {
        rabbitMqHelper.send(MqConstants.Exchange.CERT_EXCHANGE, MqConstants.Key.CERT_ISSUE_KEY, dto);
    }

    @ApiOperation("我的证书")
    @GetMapping("/my")
    public List<CertificateVO> myCertificates() {
        return certificateService.queryMyCertificates();
    }

    @ApiOperation("证书验真（公开，无需登录）")
    @GetMapping("/verify/{verifyCode}")
    public VerifyResultVO verify(@PathVariable("verifyCode") String verifyCode) {
        return certificateService.verify(verifyCode);
    }

    @ApiOperation("证书详情（仅本人）")
    @GetMapping("/{certNo}")
    public CertificateVO detail(@PathVariable("certNo") String certNo) {
        return certificateService.queryByCertNo(certNo);
    }
}
