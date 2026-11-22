package com.zhikao.learning.controller;


import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.learning.domain.query.ReplyPageQuery;
import com.zhikao.learning.domain.vo.ReplyVO;
import com.zhikao.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * <p>
 * 互动问题的回答或评论 管理端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/admin/replies")
@RequiredArgsConstructor
public class InteractionReplyAdminController {

    private final IInteractionReplyService interactionReplyService;

    /**
     * 分页查询回答或评论
     * @param replyPageQuery
     * @return
     */
    @GetMapping("/page")
    public PageDTO<ReplyVO> queryReplyPage(@Valid ReplyPageQuery replyPageQuery) {
        return interactionReplyService.queryReplyPage(replyPageQuery, true);
    }

    /**
     * 根据id查询回答或评论
     * @param id 回答或评论id
     * @return
     */
    @GetMapping("/{id}")
    public ReplyVO queryReplyById(@PathVariable("id") Long id) {
        return interactionReplyService.queryReplyById(id);
    }

    /**
     * 隐藏或显示回答或评论
     * @param id 回答或评论id
     * @param hidden 是否隐藏
     */
    @PutMapping("/{id}/hidden/{hidden}")
    public void hiddenReply(@PathVariable("id") Long id,
                            @PathVariable("hidden") Boolean hidden) {
        interactionReplyService.hiddenReply(id, hidden);
    }
}
