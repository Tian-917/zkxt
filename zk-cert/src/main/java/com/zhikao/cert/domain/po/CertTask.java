package com.zhikao.cert.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 发证补偿任务（本地消息表）
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("cert_task")
public class CertTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 业务类型：ISSUE */
    private String bizType;

    /** 业务id（考试记录id） */
    private Long bizId;

    /** 失败时的原始消息 JSON */
    private String payload;

    /** 0待处理 1成功 2失败 */
    private Integer status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
