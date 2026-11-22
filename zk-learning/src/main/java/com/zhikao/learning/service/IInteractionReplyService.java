package com.zhikao.learning.service;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.learning.domain.dto.ReplyDTO;
import com.zhikao.learning.domain.po.InteractionReply;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.learning.domain.query.ReplyPageQuery;
import com.zhikao.learning.domain.vo.ReplyVO;

/**
 * <p>
 * 互动问题的回答或评论 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IInteractionReplyService extends IService<InteractionReply> {

    void saveReply(ReplyDTO replyDTO);

    PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery replyPageQuery, boolean isAdmin);

    void hiddenReply(Long id, Boolean hidden);

    ReplyVO queryReplyById(Long id);
}
