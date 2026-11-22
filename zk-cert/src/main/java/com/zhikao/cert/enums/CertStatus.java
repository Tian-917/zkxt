package com.zhikao.cert.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 证书状态
 */
@Getter
public enum CertStatus {

    ISSUED(1, "已签发"),
    REVOKED(2, "已作废");

    @EnumValue
    private final int value;
    private final String desc;

    CertStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
