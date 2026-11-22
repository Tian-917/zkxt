package com.zhikao.learning.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.api.client.remark.RemarkClient;
import com.zhikao.api.client.user.UserClient;
import com.zhikao.api.dto.user.UserDTO;
import com.zhikao.common.autoconfigure.mq.RabbitMqHelper;
import com.zhikao.common.constants.MqConstants;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.exceptions.BadRequestException;
import com.zhikao.common.exceptions.DbException;
import com.zhikao.common.utils.BeanUtils;
import com.zhikao.common.utils.BooleanUtils;
import com.zhikao.common.utils.CollUtils;
import com.zhikao.common.utils.UserContext;
import com.zhikao.learning.domain.dto.ReplyDTO;
import com.zhikao.learning.domain.po.InteractionQuestion;
import com.zhikao.learning.domain.po.InteractionReply;
import com.zhikao.learning.domain.query.ReplyPageQuery;
import com.zhikao.learning.domain.vo.ReplyVO;
import com.zhikao.learning.enums.QuestionStatus;
import com.zhikao.learning.mapper.InteractionQuestionMapper;
import com.zhikao.learning.mapper.InteractionReplyMapper;
import com.zhikao.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final InteractionQuestionMapper questionMapper;
    private final UserClient userClient;
    private final RemarkClient remarkClient;
    private final RabbitMqHelper mqHelper;

    @Override
    @Transactional
    public void saveReply(ReplyDTO replyDTO) {
        // 1.获取当前用户
        Long userId = UserContext.getUser();
        // 2.判断是回答还是评论
        boolean isAnswer = replyDTO.getAnswerId() == null || replyDTO.getAnswerId() == 0;
        // isStudent 默认 true
        boolean isStudent = Boolean.TRUE.equals(replyDTO.getIsStudent());
        // 3.封装并保存回复
        InteractionReply reply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
        reply.setUserId(userId);
        if (isAnswer) {
            // 一级回答统一使用 0，便于按问题查询回答时与评论区分
            reply.setAnswerId(0L);
        }
        if (!save(reply)) {
            throw new DbException("保存回答失败");
        }
        if (isAnswer) {
            // 4.回答：问题回答数 +1、更新最新回答 id；学生提交则重置为未查看
            int rows = questionMapper.update(null, Wrappers.<InteractionQuestion>lambdaUpdate()
                    .setSql("answer_times = answer_times + 1")
                    .set(InteractionQuestion::getLatestAnswerId, reply.getId())
                    .set(isStudent, InteractionQuestion::getStatus, QuestionStatus.UN_CHECK)
                    .eq(InteractionQuestion::getId, replyDTO.getQuestionId()));
            if (rows == 0) {
                throw new BadRequestException("问题不存在");
            }
        } else {
            // 5.评论：被回复的那条回答评论数 +1
            boolean updated = lambdaUpdate()
                    .setSql("reply_times = reply_times + 1")
                    .eq(InteractionReply::getId, replyDTO.getAnswerId())
                    .update();
            if (!updated) {
                throw new BadRequestException("回复的回答不存在");
            }
            // 5.1 学生提交的评论，把问题重置为未查看，管理端重新看到提醒
            if (isStudent) {
                questionMapper.update(null, Wrappers.<InteractionQuestion>lambdaUpdate()
                        .set(InteractionQuestion::getStatus, QuestionStatus.UN_CHECK)
                        .eq(InteractionQuestion::getId, replyDTO.getQuestionId()));
            }
        }
        // 6.学生提交回答或评论后累加问答积分
        if (isStudent) {
            mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE, MqConstants.Key.WRITE_REPLY, userId);
        }
    }

    @Override
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query, boolean isAdmin) {
        // 1.参数校验：answerId 为 0 表示查询问题下的一级回答，否则表示查询某条回答下的评论
        Long questionId = query.getQuestionId();
        Long answerId = query.getAnswerId();
        if (questionId == null && !(answerId != null && answerId != 0)) {
            throw new BadRequestException("问题id和回答id不能同时为空");
        }
        // 2.分页查询：查询回答时限定 answer_id = 0，避免把评论也查出来
        Page<InteractionReply> page = lambdaQuery()
                .eq(questionId != null, InteractionReply::getQuestionId, questionId)
                .eq(answerId != null && answerId != 0, InteractionReply::getAnswerId, answerId)
                .eq(!(answerId != null && answerId != 0), InteractionReply::getAnswerId, 0)
                .eq(!isAdmin, InteractionReply::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionReply> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.批量查询用户信息，避免循环内逐条调用 Feign
        Set<Long> userIds = new HashSet<>();
        Set<Long> targetUserIds = new HashSet<>();
        for (InteractionReply r : records) {
            if (BooleanUtils.isFalse(r.getAnonymity())) {
                userIds.add(r.getUserId());
            }
            targetUserIds.add(r.getTargetUserId());
        }
        Map<Long, UserDTO> userMap = queryUserMap(userIds);
        Map<Long, UserDTO> targetUserMap = queryUserMap(targetUserIds);
        // 4.批量查询当前用户对回答/评论的点赞状态，避免循环内逐条调用 Feign
        Set<Long> replyIds = records.stream()
                .map(InteractionReply::getId)
                .collect(Collectors.toSet());
        Set<Long> likedIds = remarkClient.isBizLiked(replyIds);
        // 5.封装VO
        List<ReplyVO> voList = new ArrayList<>(records.size());
        for (InteractionReply r : records) {
            ReplyVO replyVO = BeanUtils.copyBean(r, ReplyVO.class);
            voList.add(replyVO);
            // 5.1.回答者信息，匿名时不返回
            if (BooleanUtils.isFalse(r.getAnonymity())) {
                UserDTO user = userMap.get(r.getUserId());
                if (user != null) {
                    replyVO.setUserName(user.getName());
                    replyVO.setUserIcon(user.getIcon());
                    replyVO.setUserType(user.getType());
                }
            } else {
                // 匿名回答不暴露真实用户id
                replyVO.setUserId(null);
            }
            // 5.2.被回复的目标用户名称
            UserDTO targetUser = targetUserMap.get(r.getTargetUserId());
            if (targetUser != null) {
                replyVO.setTargetUserName(targetUser.getName());
            }
            // 5.3.当前用户是否点过赞
            replyVO.setLiked(likedIds.contains(r.getId()));
        }
        return PageDTO.of(page, voList);
    }

    @Override
    @Transactional
    public void hiddenReply(Long id, Boolean hidden) {
        // 1.查询回答或评论
        InteractionReply old = getById(id);
        if (old == null) {
            return;
        }
        // 2.更新当前回答或评论的隐藏状态
        lambdaUpdate()
                .eq(InteractionReply::getId, id)
                .set(InteractionReply::getHidden, hidden)
                .update();
        // 3.当前是一级回答时，下属评论同步隐藏或显示
        if (old.getAnswerId() == null || old.getAnswerId() == 0) {
            lambdaUpdate()
                    .eq(InteractionReply::getAnswerId, id)
                    .set(InteractionReply::getHidden, hidden)
                    .update();
        }
    }

    @Override
    public ReplyVO queryReplyById(Long id) {
        // 1.查询回答或评论
        InteractionReply reply = getById(id);
        if (reply == null) {
            throw new BadRequestException("回答或评论不存在");
        }
        // 2.查询回复者和被回复者信息
        Set<Long> userIds = new HashSet<>();
        userIds.add(reply.getUserId());
        if (reply.getTargetUserId() != null) {
            userIds.add(reply.getTargetUserId());
        }
        if (reply.getTargetReplyId() != null && reply.getTargetReplyId() != 0) {
            InteractionReply target = getById(reply.getTargetReplyId());
            if (target != null && !Boolean.TRUE.equals(target.getAnonymity())) {
                userIds.add(target.getUserId());
            }
        }
        Map<Long, UserDTO> userMap = queryUserMap(userIds);
        Set<Long> likedIds = remarkClient.isBizLiked(CollUtils.singletonList(id));
        // 3.封装VO
        ReplyVO vo = BeanUtils.copyBean(reply, ReplyVO.class);
        UserDTO user = userMap.get(reply.getUserId());
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
            vo.setUserType(user.getType());
        }
        UserDTO targetUser = userMap.get(reply.getTargetUserId());
        if (targetUser != null) {
            vo.setTargetUserName(targetUser.getName());
        }
        vo.setLiked(likedIds.contains(id));
        return vo;
    }

    /**
     * 批量查询用户信息并转为 id -> 用户 的 Map
     */
    private Map<Long, UserDTO> queryUserMap(Set<Long> ids) {
        ids.remove(null);
        if (CollUtils.isEmpty(ids)) {
            return CollUtils.emptyMap();
        }
        List<UserDTO> users = userClient.queryUserByIds(ids);
        if (CollUtils.isEmpty(users)) {
            return CollUtils.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
    }

}
