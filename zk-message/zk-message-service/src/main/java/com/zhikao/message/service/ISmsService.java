package com.zhikao.message.service;

import com.zhikao.api.dto.sms.SmsInfoDTO;
import com.zhikao.api.dto.user.UserDTO;
import com.zhikao.message.domain.po.NoticeTemplate;

import java.util.List;

public interface ISmsService {
    void sendMessageByTemplate(NoticeTemplate noticeTemplate, List<UserDTO> users);

    void sendMessage(SmsInfoDTO smsInfoDTO);

    void sendMessageAsync(SmsInfoDTO smsInfoDTO);
}
