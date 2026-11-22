package com.zhikao.trade.domain.query;

import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.common.utils.DateUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "退款申请分页参数")
public class RefundApplyPageQuery extends PageQuery {

    @ApiModelProperty("是否只看我的：学生端传 true，管理端不传表示查全部")
    private Boolean mine;

    @ApiModelProperty("退款id")
    private Long id;    @ApiModelProperty("退款状态，1：待审批，2：取消退款，3：同意退款，4：拒绝退款，5：退款成功，6：退款失败")
    private Integer refundStatus;
    @ApiModelProperty("订单明细id")
    private Long orderDetailId;
    @ApiModelProperty("订单id")
    private Long orderId;
    @ApiModelProperty("学员手机号")
    private String mobile;
    @ApiModelProperty("申请开始时间")
    @DateTimeFormat(pattern = DateUtils.DEFAULT_DATE_TIME_FORMAT)
    private LocalDateTime applyStartTime;
    @ApiModelProperty("申请结束时间")
    @DateTimeFormat(pattern = DateUtils.DEFAULT_DATE_TIME_FORMAT)
    private LocalDateTime applyEndTime;
}
