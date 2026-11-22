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
 * 证书
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("certificate")
public class Certificate implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 证书编号 ZK-YYYY-NNNNNN */
    private String certNo;

    /** 持证人id */
    private Long userId;

    /** 持证人姓名 */
    private String userName;

    /** 证书类型 */
    private String certType;

    /** 来源考试记录id */
    private Long examId;

    /** 考核成绩 */
    private Integer score;

    /** 状态：1已签发 2已作废 */
    private Integer status;

    /** 签发时间 */
    private LocalDateTime issueTime;

    /** 有效期至 */
    private LocalDateTime expireTime;

    /** 验真短码 */
    private String verifyCode;

    /** 是否已做到期提醒 */
    private Integer notified;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
