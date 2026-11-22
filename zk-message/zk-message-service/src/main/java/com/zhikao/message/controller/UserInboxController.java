package com.zhikao.message.controller;


import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.message.domain.dto.UserInboxDTO;
import com.zhikao.message.domain.dto.UserInboxFormDTO;
import com.zhikao.message.domain.query.UserInboxQuery;
import com.zhikao.message.service.IUserInboxService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户通知记录 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Api(tags = "用户收件箱接口")
@RestController
@RequestMapping("/inboxes")
@RequiredArgsConstructor
public class UserInboxController {

    private final IUserInboxService inboxService;

    @PostMapping
    @ApiOperation("发送私信")
    public Long sentMessageToUser(@RequestBody UserInboxFormDTO userInboxFormDTO){
        return inboxService.sentMessageToUser(userInboxFormDTO);
    }

    @ApiOperation("分页查询收件箱")
    @GetMapping
    public PageDTO<UserInboxDTO> queryUserInBoxesPage(UserInboxQuery query){
        return inboxService.queryUserInBoxesPage(query);
    }

    @ApiOperation("标记单条已读")
    @PutMapping("/{id}/read")
    public int markRead(@PathVariable("id") Long id){
        return inboxService.markRead(id);
    }

    @ApiOperation("全部标记已读")
    @PutMapping("/read-all")
    public int markAllRead(){
        return inboxService.markAllRead();
    }

    @ApiOperation("未读数量")
    @GetMapping("/unread-count")
    public int unreadCount(){
        return inboxService.unreadCount();
    }
}
