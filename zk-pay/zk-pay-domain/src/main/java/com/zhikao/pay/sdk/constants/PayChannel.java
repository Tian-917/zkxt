package com.zhikao.pay.sdk.constants;

import com.zhikao.common.utils.StringUtils;
import lombok.Getter;

@Getter
public enum PayChannel {
    wxPay("微信支付"),
    aliPay("支付宝支付"),
    ;

    private final String desc;

    PayChannel(String desc) {
        this.desc = desc;
    }

    public static String desc(String value){
        if (StringUtils.isBlank(value)) {
            return "";
        }
        try {
            return PayChannel.valueOf(value).getDesc();
        } catch (IllegalArgumentException e) {
            // 未知渠道返回原值
            return value;
        }
    }
}
