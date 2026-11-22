package com.zhikao.api.client.message;

import com.zhikao.api.dto.message.InboxMessageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 消息中心客户端
 * <p>证书到期提醒等场景用它给用户发站内信</p>
 */
@FeignClient(value = "message-service", contextId = "inboxMessageClient")
public interface MessageClient {

    /**
     * 发送站内信
     *
     * @param dto 收件人 + 内容
     * @return 站内信 id
     */
    @PostMapping("/inboxes")
    Long sendInbox(@RequestBody InboxMessageDTO dto);
}
