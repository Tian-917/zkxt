package com.zhikao.api.dto.cert;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 发证申请（考核通过事件载荷）
 * <p>考试服务判分通过后发出，证书域消费；放在 api 模块因为两端都要用</p>
 */
@Data
@Accessors(chain = true)
public class CertIssueDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 持证人id */
    private Long userId;

    /** 持证人姓名 */
    private String userName;

    /** 证书类型（用课包名） */
    private String certType;

    /** 来源考试记录id（同一场考试只发一张证） */
    private Long examId;

    /** 考核成绩 */
    private Integer score;

    /** 累计学时（分钟），由学习服务按已完成小节汇总 */
    private Integer studyMinutes;
}
