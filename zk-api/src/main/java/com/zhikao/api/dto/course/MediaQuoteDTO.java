package com.zhikao.api.dto.course;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName MediaQuoteDTO
 * @author Tian
 * @since 2026-11-19
 * @version 1.0.0
 **/
@ApiModel("媒资被引用情况")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaQuoteDTO {
    @ApiModelProperty("媒资id")
    private Long mediaId;
    @ApiModelProperty("引用数")
    private Integer quoteNum;
}
