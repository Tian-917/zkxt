package com.zhikao.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.message.domain.po.UserInbox;

/**
 * <p>
 * 用户通知记录 Mapper 接口
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface UserInboxMapper extends BaseMapper<UserInbox> {

    UserInbox queryLatestPublicNotice(Long userId);
}
