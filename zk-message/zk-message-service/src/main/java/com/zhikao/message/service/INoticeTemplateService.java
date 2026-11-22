package com.zhikao.message.service;

import com.zhikao.message.domain.dto.NoticeTemplateDTO;
import com.zhikao.message.domain.dto.NoticeTemplateFormDTO;
import com.zhikao.message.domain.query.NoticeTemplatePageQuery;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.message.domain.po.NoticeTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 通知模板 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface INoticeTemplateService extends IService<NoticeTemplate> {

    Long saveNoticeTemplate(NoticeTemplateFormDTO noticeTemplateFormDTO);

    void updateNoticeTemplate(NoticeTemplateFormDTO noticeTemplateFormDTO);

    PageDTO<NoticeTemplateDTO> queryNoticeTemplates(NoticeTemplatePageQuery pageQuery);

    NoticeTemplateDTO queryNoticeTemplate(Long id);

    NoticeTemplate queryByCode(String code);
}
