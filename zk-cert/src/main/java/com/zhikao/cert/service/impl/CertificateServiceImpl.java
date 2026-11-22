package com.zhikao.cert.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.cert.constants.CertConstants;
import com.zhikao.api.dto.cert.CertIssueDTO;
import com.zhikao.api.client.message.MessageClient;
import com.zhikao.api.dto.message.InboxMessageDTO;
import com.zhikao.cert.domain.po.CertTask;
import com.zhikao.cert.domain.po.Certificate;
import com.zhikao.cert.domain.vo.CertificateVO;
import com.zhikao.cert.domain.vo.VerifyResultVO;
import com.zhikao.cert.enums.CertStatus;
import com.zhikao.cert.mapper.CertTaskMapper;
import com.zhikao.cert.mapper.CertificateMapper;
import com.zhikao.cert.service.ICertificateService;
import com.zhikao.common.exceptions.BadRequestException;
import com.zhikao.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 证书服务实现
 * <p>
 * 三个关键设计：
 * 1. 发证幂等：uk(user_id, exam_id) 唯一索引兜底，先查后插，冲突再查
 * 2. 编号唯一：Redis 号段自增 + 数据库唯一索引二次兜底，冲突换号重试
 * 3. 最终一致：发证失败落 cert_task，由 XXL-Job 补偿重放
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements ICertificateService {

    private final CertificateMapper certificateMapper;
    private final CertTaskMapper certTaskMapper;
    private final StringRedisTemplate redisTemplate;
    private final MessageClient messageClient;

    @Override
    public CertificateVO issue(CertIssueDTO dto) {
        // 1. 资格判定：成绩 + 学时双达标
        checkQualified(dto);

        // 2. 幂等：同一场考试只发一张
        Certificate exists = getByUserAndExam(dto.getUserId(), dto.getExamId());
        if (exists != null) {
            log.info("证书已存在，直接返回：userId={}, examId={}, certNo={}",
                    dto.getUserId(), dto.getExamId(), exists.getCertNo());
            return toVO(exists);
        }

        // 3. 签发：编号冲突 / 并发重复都由唯一索引兜底，最多重试 3 次
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < CertConstants.CERT_NO_RETRY; i++) {
            Certificate cert = buildCertificate(dto, now);
            cert.setCertNo(nextCertNo(now.getYear()));
            try {
                certificateMapper.insert(cert);
                log.info("发证成功：userId={}, examId={}, certNo={}",
                        dto.getUserId(), dto.getExamId(), cert.getCertNo());
                return toVO(cert);
            } catch (DuplicateKeyException e) {
                // 3.1 并发下同一场考试重复发证：查既有记录返回（幂等）
                Certificate duplicate = getByUserAndExam(dto.getUserId(), dto.getExamId());
                if (duplicate != null) {
                    log.info("并发重复发证，返回既有证书：{}", duplicate.getCertNo());
                    return toVO(duplicate);
                }
                // 3.2 否则是编号冲突，换号重试
                log.warn("证书编号冲突，第 {} 次重试", i + 1);
            }
        }
        throw new BadRequestException("证书编号生成冲突，请稍后重试");
    }

    @Override
    public void handleExamPassed(CertIssueDTO dto) {
        try {
            issue(dto);
            finishTask(dto.getExamId());
        } catch (BadRequestException e) {
            // 资格不达标属于业务拒绝，重试没有意义
            log.warn("发证被拒绝：examId={}, reason={}", dto.getExamId(), e.getMessage());
        } catch (Exception e) {
            // 系统异常（Redis/DB/MQ）才落补偿表
            log.error("发证异常，落补偿任务：examId={}", dto.getExamId(), e);
            saveTask(dto);
        }
    }

    @Override
    public List<CertificateVO> queryMyCertificates() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Certificate> list = certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getUserId, userId)
                .eq(Certificate::getStatus, CertStatus.ISSUED.getValue())
                .orderByDesc(Certificate::getIssueTime));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public CertificateVO queryByCertNo(String certNo) {
        Certificate cert = certificateMapper.selectOne(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getCertNo, certNo));
        if (cert == null) {
            throw new BadRequestException("证书不存在");
        }
        Long userId = UserContext.getUser();
        if (userId != null && !userId.equals(cert.getUserId())) {
            throw new BadRequestException("无权查看他人证书");
        }
        return toVO(cert);
    }

    @Override
    public VerifyResultVO verify(String verifyCode) {
        Certificate cert = certificateMapper.selectOne(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getVerifyCode, verifyCode));
        if (cert == null) {
            return new VerifyResultVO()
                    .setValid(false)
                    .setMessage("未查询到该验真码对应的证书");
        }
        boolean expired = cert.getExpireTime() != null && cert.getExpireTime().isBefore(LocalDateTime.now());
        return new VerifyResultVO()
                .setValid(!expired && CertStatus.ISSUED.getValue() == cert.getStatus())
                .setCertNo(cert.getCertNo())
                .setHolder(maskName(cert.getUserName()))
                .setCertType(cert.getCertType())
                .setIssueTime(cert.getIssueTime())
                .setExpireTime(cert.getExpireTime())
                .setMessage(expired ? "证书已过有效期，请复证" : "证书真实有效");
    }

    @Override
    public int notifyExpiring() {
        LocalDateTime deadline = LocalDateTime.now().plusDays(CertConstants.EXPIRE_NOTICE_DAYS);
        List<Certificate> list = certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getStatus, CertStatus.ISSUED.getValue())
                .eq(Certificate::getNotified, 0)
                .le(Certificate::getExpireTime, deadline));
        int sent = 0;
        for (Certificate cert : list) {
            // 发站内信；发送失败保持 notified=0，下次任务重试
            boolean ok = false;
            try {
                String content = String.format(
                        "您的《%s》证书将于 %s 到期（编号 %s），到期后需重新考核复证，请提前安排学习。",
                        cert.getCertType(), cert.getExpireTime().toLocalDate(), cert.getCertNo());
                messageClient.sendInbox(new InboxMessageDTO()
                        .setUserId(cert.getUserId())
                        .setContent(content));
                ok = true;
                sent++;
                log.info("[到期提醒] 已发送站内信 certNo={}, user={}", cert.getCertNo(), cert.getUserId());
            } catch (Exception e) {
                log.warn("[到期提醒] 站内信发送失败 certNo={}, user={}, 原因={}",
                        cert.getCertNo(), cert.getUserId(), e.getMessage());
            }
            if (ok) {
                certificateMapper.updateById(new Certificate()
                        .setId(cert.getId())
                        .setNotified(1));
            }
        }
        return sent;
    }

    @Override
    public int compensate() {
        List<CertTask> tasks = certTaskMapper.selectList(new LambdaQueryWrapper<CertTask>()
                .eq(CertTask::getBizType, CertConstants.TASK_TYPE_ISSUE)
                .in(CertTask::getStatus, CertConstants.TASK_STATUS_PENDING, CertConstants.TASK_STATUS_FAIL)
                .lt(CertTask::getRetryCount, CertConstants.TASK_MAX_RETRY));
        int success = 0;
        for (CertTask task : tasks) {
            CertIssueDTO dto = toDTO(task);
            try {
                issue(dto);
                certTaskMapper.updateById(new CertTask()
                        .setId(task.getId())
                        .setStatus(CertConstants.TASK_STATUS_SUCCESS));
                success++;
            } catch (Exception e) {
                log.warn("补偿发证仍失败：examId={}, retry={}", task.getBizId(), task.getRetryCount() + 1);
                certTaskMapper.updateById(new CertTask()
                        .setId(task.getId())
                        .setStatus(CertConstants.TASK_STATUS_FAIL)
                        .setRetryCount(task.getRetryCount() + 1)
                        .setNextRetryTime(LocalDateTime.now().plusMinutes(5)));
            }
        }
        return success;
    }

    /* ================= 私有方法 ================= */

    private void checkQualified(CertIssueDTO dto) {
        if (dto.getScore() == null || dto.getScore() < CertConstants.PASS_SCORE) {
            throw new BadRequestException("考核成绩未达到 " + CertConstants.PASS_SCORE + " 分，不予签发");
        }
        if (dto.getStudyMinutes() == null || dto.getStudyMinutes() < CertConstants.REQUIRED_STUDY_MINUTES) {
            throw new BadRequestException("累计学时未达到 " + CertConstants.REQUIRED_STUDY_MINUTES + " 分钟，不予签发");
        }
    }

    private Certificate getByUserAndExam(Long userId, Long examId) {
        return certificateMapper.selectOne(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getUserId, userId)
                .eq(Certificate::getExamId, examId));
    }

    private Certificate buildCertificate(CertIssueDTO dto, LocalDateTime now) {
        return new Certificate()
                .setUserId(dto.getUserId())
                .setUserName(dto.getUserName() == null ? "" : dto.getUserName())
                .setCertType(dto.getCertType())
                .setExamId(dto.getExamId())
                .setScore(dto.getScore())
                .setStatus(CertStatus.ISSUED.getValue())
                .setIssueTime(now)
                .setExpireTime(now.plusYears(CertConstants.VALID_YEARS))
                .setVerifyCode(RandomUtil.randomString(12))
                .setNotified(0);
    }

    /**
     * 证书编号：ZK-{年}-{6位序号}
     * Redis 号段自增保证唯一，宕机后序号可能回退，由 DB 唯一索引 + 重试兜底
     */
    private String nextCertNo(int year) {
        Long seq = redisTemplate.opsForValue().increment(CertConstants.CERT_SEQ_KEY + year);
        if (seq == null) {
            throw new BadRequestException("证书编号生成失败");
        }
        return String.format("ZK-%d-%06d", year, seq);
    }

    private void saveTask(CertIssueDTO dto) {
        CertTask exists = certTaskMapper.selectOne(new LambdaQueryWrapper<CertTask>()
                .eq(CertTask::getBizType, CertConstants.TASK_TYPE_ISSUE)
                .eq(CertTask::getBizId, dto.getExamId()));
        CertTask task = new CertTask()
                .setBizType(CertConstants.TASK_TYPE_ISSUE)
                .setBizId(dto.getExamId())
                .setPayload(String.format("%d|%s|%s|%d|%d",
                        dto.getUserId(), dto.getUserName(), dto.getCertType(),
                        dto.getScore(), dto.getStudyMinutes()))
                .setStatus(CertConstants.TASK_STATUS_PENDING)
                .setRetryCount(0)
                .setNextRetryTime(LocalDateTime.now().plusMinutes(1));
        if (exists == null) {
            certTaskMapper.insert(task);
        } else {
            task.setId(exists.getId());
            certTaskMapper.updateById(task);
        }
    }

    private void finishTask(Long examId) {
        CertTask task = certTaskMapper.selectOne(new LambdaQueryWrapper<CertTask>()
                .eq(CertTask::getBizType, CertConstants.TASK_TYPE_ISSUE)
                .eq(CertTask::getBizId, examId));
        if (task != null && task.getStatus() != CertConstants.TASK_STATUS_SUCCESS) {
            certTaskMapper.updateById(new CertTask()
                    .setId(task.getId())
                    .setStatus(CertConstants.TASK_STATUS_SUCCESS));
        }
    }

    private CertIssueDTO toDTO(CertTask task) {
        String[] parts = task.getPayload() == null ? new String[0] : task.getPayload().split("\\|");
        return new CertIssueDTO()
                .setUserId(Long.valueOf(parts[0]))
                .setUserName(parts[1])
                .setCertType(parts[2])
                .setExamId(task.getBizId())
                .setScore(Integer.valueOf(parts[3]))
                .setStudyMinutes(Integer.valueOf(parts[4]));
    }

    private CertificateVO toVO(Certificate cert) {
        long remainDays = cert.getExpireTime() == null ? 0 :
                Duration.between(LocalDateTime.now(), cert.getExpireTime()).toDays();
        return new CertificateVO()
                .setCertNo(cert.getCertNo())
                .setUserName(cert.getUserName())
                .setCertType(cert.getCertType())
                .setScore(cert.getScore())
                .setStatus(cert.getStatus())
                .setIssueTime(cert.getIssueTime())
                .setExpireTime(cert.getExpireTime())
                .setRemainDays(remainDays)
                .setVerifyCode(cert.getVerifyCode());
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "*";
        }
        return name.charAt(0) + (name.length() > 1 ? "*" : "");
    }
}
