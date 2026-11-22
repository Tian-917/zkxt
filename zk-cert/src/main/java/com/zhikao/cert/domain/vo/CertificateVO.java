package com.zhikao.cert.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 证书视图
 */
@Data
@Accessors(chain = true)
@ApiModel(description = "证书信息")
public class CertificateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("证书编号")
    private String certNo;

    @ApiModelProperty("持证人姓名")
    private String userName;

    @ApiModelProperty("证书类型")
    private String certType;

    @ApiModelProperty("考核成绩")
    private Integer score;

    @ApiModelProperty("状态：1已签发 2已作废")
    private Integer status;

    @ApiModelProperty("签发时间")
    private LocalDateTime issueTime;

    @ApiModelProperty("有效期至")
    private LocalDateTime expireTime;

    @ApiModelProperty("剩余有效天数")
    private Long remainDays;

    @ApiModelProperty("验真码（扫码/输入后可验真）")
    private String verifyCode;

    @ApiModelProperty("是否已过期")
    public Boolean getExpired() {
        return expireTime != null && expireTime.isBefore(LocalDateTime.now());
    }

    @ApiModelProperty("签发日期（展示用）")
    public LocalDate getIssueDate() {
        return issueTime == null ? null : issueTime.toLocalDate();
    }
}
