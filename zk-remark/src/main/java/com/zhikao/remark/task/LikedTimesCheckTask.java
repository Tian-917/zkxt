package com.zhikao.remark.task;


import com.zhikao.remark.config.LikedTimesProperties;
import com.zhikao.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LikedTimesCheckTask {

    private final LikedTimesProperties likedTimesProperties;
    private final ILikedRecordService recordService;

    @Scheduled(fixedDelay = 20000)
    public void checkLikedTimes() {
        for (String bizType : likedTimesProperties.getBizTypes()) {
            recordService.readLikedTimesAndSendMessage(bizType, likedTimesProperties.getMaxBizSize());
        }
    }
}
