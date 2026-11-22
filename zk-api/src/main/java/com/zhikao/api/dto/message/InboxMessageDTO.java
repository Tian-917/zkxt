package com.zhikao.api.dto.message;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 站内信发送表单（字段名与 message-service 的 UserInboxFormDTO 对齐，Feign 走 JSON 序列化）
 */
@Data
@Accessors(chain = true)
public class InboxMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标用户id */
    private Long userId;

    /** 私信内容 */
    private String content;
}
