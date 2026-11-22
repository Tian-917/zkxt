package com.zhikao.pay.controller;


import com.zhikao.common.exceptions.BadRequestException;
import com.zhikao.pay.sdk.constants.PayErrorInfo;
import com.zhikao.pay.sdk.constants.PayType;
import com.zhikao.pay.sdk.dto.PayApplyDTO;
import com.zhikao.pay.sdk.dto.PayResultDTO;
import com.zhikao.pay.service.IPayOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 支付订单 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Api(tags = "支付相关接口")
@RestController
@RequestMapping("/pay-orders")
@RequiredArgsConstructor
public class PayOrderController {

    private final IPayOrderService payOrderService;

    @ApiOperation("扫码支付申请支付单，返回支付url地址，用于生产二维码")
    @PostMapping
    public String applyPayOrder(@RequestBody PayApplyDTO payApplyDTO){
        if(!PayType.NATIVE.equalsValue(payApplyDTO.getPayType())){
            throw new BadRequestException(PayErrorInfo.INVALID_PAY_TYPE);
        }
        return payOrderService.applyPayOrder(payApplyDTO);
    }

    @ApiOperation("根据业务端订单id查询支付结果")
    @GetMapping("/{bizOrderId}/status")
    public PayResultDTO queryPayResult(
            @ApiParam("业务订单id") @PathVariable("bizOrderId") Long bizOrderId
    ){
        return payOrderService.queryPayResult(bizOrderId);
    }

    @ApiOperation("模拟支付成功（无商户凭证时用于本地演示，等价于第三方回调）")
    @PostMapping("/{bizOrderNo}/mock-pay")
    public boolean mockPay(@ApiParam("业务订单id") @PathVariable("bizOrderNo") Long bizOrderNo) {
        return payOrderService.mockPaySuccess(bizOrderNo);
    }
}
