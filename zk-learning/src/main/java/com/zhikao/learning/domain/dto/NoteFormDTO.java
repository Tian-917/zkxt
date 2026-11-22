package com.zhikao.learning.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@ApiModel(description = "笔记信息")
public class NoteFormDTO {

    /**
     * 新增笔记时的校验分组
     */
    public interface Save {
    }

    /**
     * 更新笔记时的校验分组
     */
    public interface Update {
    }

    @ApiModelProperty("笔记id，更新笔记时使用")
    private Long id;
    @NotBlank(groups = {Save.class, Update.class}, message = "笔记内容不能为空")
    @ApiModelProperty("笔记内容")
    private String content;
    @NotNull(groups = Save.class, message = "笔记时间不能为空")
    @ApiModelProperty("记录笔记时的视频播放时间，单位秒")
    private Integer noteMoment;
    @NotNull(groups = Save.class, message = "隐私标记不能为空")
    @ApiModelProperty("是否是隐私笔记，默认false")
    private Boolean isPrivate;
    @NotNull(groups = Save.class, message = "课程id不能为空")
    @ApiModelProperty("课程id")
    private Long courseId;
    @NotNull(groups = Save.class, message = "章id不能为空")
    @ApiModelProperty("章id")
    private Long chapterId;
    @NotNull(groups = Save.class, message = "节id不能为空")
    @ApiModelProperty("小节id")
    private Long sectionId;
}
