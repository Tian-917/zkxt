package com.zhikao.remark.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 点赞数统计任务的配置。
 * 注册到 Nacos 配置中心后支持热更新，无需重启服务。
 */
@Data
@Component
@ConfigurationProperties(prefix = "zk.remark.liked-times")
public class LikedTimesProperties {

    /**
     * 需要统计点赞的业务类型
     */
    private List<String> bizTypes = List.of("QA", "NOTE");

    /**
     * 每次批量处理的业务数量上限
     */
    private int maxBizSize = 30;
}
