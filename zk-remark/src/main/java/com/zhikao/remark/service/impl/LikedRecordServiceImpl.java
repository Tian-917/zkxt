package com.zhikao.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhikao.api.dto.remark.LikedTimesDTO;
import com.zhikao.common.autoconfigure.mq.RabbitMqHelper;
import com.zhikao.common.utils.StringUtils;
import com.zhikao.common.utils.UserContext;
import com.zhikao.remark.domain.dto.LikeRecordFormDTO;
import com.zhikao.remark.domain.po.LikedRecord;
import com.zhikao.remark.mapper.LikedRecordMapper;
import com.zhikao.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.zhikao.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.zhikao.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
//@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final RabbitMqHelper mqHelper;

    @Override
    public void addLikeRecord(LikeRecordFormDTO recordDTO) {
        //1.基于前端的参数，判断是执行点赞还是取消点赞
        Boolean success = recordDTO.getLiked() ? like(recordDTO) : unlike(recordDTO);
        //2.判断是否执行成功，如果失败，则直接结束
        if(!success){
            return;
        }
        //3.如果执行成功，统计点赞总数
        Integer likeTimes = lambdaQuery()
                .eq(LikedRecord::getBizId, recordDTO.getBizId())
                .count();
        // 4.发送MQ通知
        mqHelper.send(
                LIKE_RECORD_EXCHANGE,
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, recordDTO.getBizType()),
                LikedTimesDTO.of(recordDTO.getBizId(), likeTimes));
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        // 1.获取登录用户id
        Long userId = UserContext.getUser();
        // 2.查询点赞状态
        List<LikedRecord> list = lambdaQuery()
                .in(LikedRecord::getBizId, bizIds)
                .eq(LikedRecord::getUserId, userId)
                .list();
        // 3.返回结果
        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {

    }

    private Boolean unlike(LikeRecordFormDTO recordDTO) {
        return remove(new QueryWrapper<LikedRecord>().lambda()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, recordDTO.getBizId()));
    }

    private Boolean like(LikeRecordFormDTO recordDTO) {
        Long userId = UserContext.getUser();
        // 1.查询点赞记录
        Integer count = lambdaQuery()
                .eq(LikedRecord::getUserId, userId)
                .eq(LikedRecord::getBizId, recordDTO.getBizId())
                .count();
        // 2.判断是否存在，如果不存在直接结束
        if(count > 0){
            return false;
        }
        // 3.如果存在，直接新增
        LikedRecord record = new LikedRecord();
        record.setUserId(userId);
        record.setBizType(recordDTO.getBizType());
        record.setBizId(recordDTO.getBizId());
        save(record);
        return true;
    }
}
