package com.zhikao.api.dto.course;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小节信息：所属课程 id、媒体 id
 * <p>
 * 注意：项目全局把 Long 序列化成字符串（防止前端精度丢失），但这份 DTO 只用于服务之间的
 * Feign 调用，必须保持数字形态，否则反序列化回 Long 时会变成 null。
 */
@Data
@ApiModel("小节信息：所属课程id和媒体id")
@AllArgsConstructor
@NoArgsConstructor
public class SectionInfoDTO {
    @ApiModelProperty("课程id")
    @JsonSerialize(using = RawLongSerializer.class)
    private Long courseId;

    @ApiModelProperty("媒体id")
    @JsonSerialize(using = RawLongSerializer.class)
    private Long mediaId;

    @ApiModelProperty("是否支持试看")
    private Boolean trailer;

    @ApiModelProperty("访问时长，试看时不为0，单位秒")
    private Integer freeDuration;
}
