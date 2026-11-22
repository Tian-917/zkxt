package com.zhikao.cert.constants;

/**
 * 证书域常量
 */
public interface CertConstants {

    /* ========== Redis ========== */
    /** 证书编号号段：zk:cert:seq:{year} */
    String CERT_SEQ_KEY = "zk:cert:seq:";

    /* ========== MQ ========== */
    /** 发证队列 */
    String CERT_ISSUE_QUEUE = "cert.issue.queue";

    /* ========== 规则 ========== */
    /** 及格线 */
    int PASS_SCORE = 60;
    /** 发证所需最低学时（分钟） */
    int REQUIRED_STUDY_MINUTES = 120;
    /** 证书有效期（年） */
    int VALID_YEARS = 5;
    /** 到期前 N 天提醒复证 */
    int EXPIRE_NOTICE_DAYS = 30;
    /** 编号冲突重试次数 */
    int CERT_NO_RETRY = 3;

    /* ========== 任务 ========== */
    String TASK_TYPE_ISSUE = "ISSUE";
    int TASK_STATUS_PENDING = 0;
    int TASK_STATUS_SUCCESS = 1;
    int TASK_STATUS_FAIL = 2;
    int TASK_MAX_RETRY = 3;
}
