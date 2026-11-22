package com.zhikao.cert.task;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.zhikao.cert.service.ICertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 证书域定时任务
 * <p>在 XXL-Job 控制台配置两个任务，JobHandler 名称与注解一致</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertJobHandler {

    private final ICertificateService certificateService;

    /**
     * 到期复证提醒：扫描 30 天内到期且未提醒的证书
     * 建议 Cron：0 0 9 * * ?（每天 9 点）
     */
    @XxlJob("certExpireNoticeJob")
    public void expireNotice() {
        int count = certificateService.notifyExpiring();
        XxlJobHelper.log("到期复证提醒完成，本次提醒 {} 张", count);
        log.info("到期复证提醒完成，本次提醒 {} 张", count);
    }

    /**
     * 发证补偿：重放 cert_task 中失败的记录
     * 建议 Cron：0 0/10 * * * ?（每 10 分钟）
     */
    @XxlJob("certIssueCompensateJob")
    public void compensate() {
        int count = certificateService.compensate();
        XxlJobHelper.log("发证补偿完成，本次成功 {} 张", count);
        log.info("发证补偿完成，本次成功 {} 张", count);
    }
}
