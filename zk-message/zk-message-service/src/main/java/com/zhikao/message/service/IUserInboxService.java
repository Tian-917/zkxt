package com.zhikao.message.service;

import com.zhikao.message.domain.dto.UserInboxDTO;
import com.zhikao.message.domain.dto.UserInboxFormDTO;
import com.zhikao.api.dto.user.UserDTO;
import com.zhikao.message.domain.query.UserInboxQuery;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.message.domain.po.NoticeTemplate;
import com.zhikao.message.domain.po.UserInbox;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 用户通知记录 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IUserInboxService extends IService<UserInbox> {

    void saveNoticeToInbox(NoticeTemplate noticeTemplate, List<UserDTO> users);

    PageDTO<UserInboxDTO> queryUserInBoxesPage(UserInboxQuery query);

    Long sentMessageToUser(UserInboxFormDTO userInboxFormDTO);

    /**
     * 标记单条站内信为已读（只能操作自己的）
     *
     * @param id 站内信id
     * @return 影响行数
     */
    int markRead(Long id);

    /**
     * 全部标记为已读
     *
     * @return 影响行数
     */
    int markAllRead();

    /**
     * 未读数量（is_read 为 null 也算未读，历史数据兼容）
     *
     * @return 未读数
     */
    int unreadCount();
}
