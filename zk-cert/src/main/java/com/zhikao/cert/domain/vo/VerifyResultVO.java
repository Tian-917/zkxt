package com.zhikao.cert.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 验真结果（公开接口返回，不含敏感信息）
 */
@Data
@Accessors(chain = true)
@ApiModel(description = "证书验真结果")
public class VerifyResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("是否真实有效")
    private Boolean valid;

    @ApiModelProperty("证书编号")
    private String certNo;

    @ApiModelProperty("持证人（脱敏：张*）")
    private String holder;

    @ApiModelProperty("证书类型")
    private String certType;

    @ApiModelProperty("签发时间")
    private LocalDateTime issueTime;

    @ApiModelProperty("有效期至")
    private LocalDateTime expireTime;

    @ApiModelProperty("状态说明")
    private String message;
}
