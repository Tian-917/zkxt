package com.zhikao.message.service;

import com.zhikao.message.domain.dto.MessageTemplateDTO;
import com.zhikao.message.domain.dto.MessageTemplateFormDTO;
import com.zhikao.message.domain.query.MessageTemplatePageQuery;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.message.domain.po.MessageTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 第三方短信平台签名和模板信息 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IMessageTemplateService extends IService<MessageTemplate> {

    List<MessageTemplate> queryByNoticeTemplateId(Long id);

    Long saveMessageTemplate(MessageTemplateFormDTO messageTemplateDTO);

    void updateMessageTemplate(MessageTemplateFormDTO messageTemplateDTO);

    PageDTO<MessageTemplateDTO> queryMessageTemplates(MessageTemplatePageQuery pageQuery);

    MessageTemplateDTO queryMessageTemplate(Long id);
}
