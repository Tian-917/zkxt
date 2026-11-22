package com.zhikao.message.service;

import com.zhikao.message.domain.po.NoticeTemplate;
import com.zhikao.message.domain.po.PublicNotice;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 公告消息模板 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IPublicNoticeService extends IService<PublicNotice> {

    void saveNoticeOfTemplate(NoticeTemplate noticeTemplate);
}
