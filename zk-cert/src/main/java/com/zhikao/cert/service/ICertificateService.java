package com.zhikao.cert.service;

import com.zhikao.api.dto.cert.CertIssueDTO;
import com.zhikao.cert.domain.vo.CertificateVO;
import com.zhikao.cert.domain.vo.VerifyResultVO;

import java.util.List;

/**
 * 证书服务
 */
public interface ICertificateService {

    /**
     * 发证：资格判定 + 幂等 + 编号唯一
     *
     * @param dto 发证申请
     * @return 证书信息（已存在则返回既有证书）
     */
    CertificateVO issue(CertIssueDTO dto);

    /**
     * 处理"考核通过"事件：失败落补偿表，由定时任务重试
     */
    void handleExamPassed(CertIssueDTO dto);

    /**
     * 查询当前登录人的全部证书
     */
    List<CertificateVO> queryMyCertificates();

    /**
     * 按证书编号查询（仅本人）
     */
    CertificateVO queryByCertNo(String certNo);

    /**
     * 公开验真：按验真码查询，不返回敏感信息
     */
    VerifyResultVO verify(String verifyCode);

    /**
     * 到期提醒：扫描 30 天内到期且未提醒的证书
     *
     * @return 本次提醒数量
     */
    int notifyExpiring();

    /**
     * 补偿重试：捞取发证失败的任务重放
     *
     * @return 本次成功补偿数量
     */
    int compensate();
}
