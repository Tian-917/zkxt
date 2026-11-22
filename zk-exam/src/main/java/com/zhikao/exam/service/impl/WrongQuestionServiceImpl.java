package com.zhikao.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.api.dto.exam.QuestionDTO;
import com.zhikao.common.exceptions.UnauthorizedException;
import com.zhikao.common.utils.CollUtils;
import com.zhikao.common.utils.UserContext;
import com.zhikao.exam.domain.po.ExamRecord;
import com.zhikao.exam.domain.po.ExamRecordDetail;
import com.zhikao.exam.domain.po.WrongQuestion;
import com.zhikao.exam.domain.vo.PointStatVO;
import com.zhikao.exam.domain.vo.WrongQuestionVO;
import com.zhikao.exam.mapper.WrongQuestionMapper;
import com.zhikao.exam.service.IQuestionService;
import com.zhikao.exam.service.IWrongQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 错题本实现
 * <p>
 * 设计要点：
 * 1. 一题一条（uk user_id + question_id），答错累加次数，不重复建行
 * 2. 重练答对自动置为已掌握，错题本会"自己变少"
 * 3. 按小节（考点）聚合，而不是按章节，直接对应课包的考点粒度
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WrongQuestionServiceImpl implements IWrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final IQuestionService questionService;

    @Override
    public void recordFromExam(ExamRecord record, List<ExamRecordDetail> details) {
        if (record == null || CollUtils.isEmpty(details)) {
            return;
        }
        int wrong = 0, mastered = 0;
        for (ExamRecordDetail detail : details) {
            if (detail.getQuestionId() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(detail.getCorrect())) {
                // 重练答对：把历史错题置为已掌握
                wrongQuestionMapper.markMastered(record.getUserId(), detail.getQuestionId());
                mastered++;
            } else {
                wrongQuestionMapper.upsertWrong(new WrongQuestion()
                        .setUserId(record.getUserId())
                        .setQuestionId(detail.getQuestionId())
                        .setCourseId(record.getCourseId())
                        .setSectionId(record.getSectionId())
                        .setSectionName(record.getSectionName())
                        .setLastAnswer(detail.getAnswer()));
                wrong++;
            }
        }
        log.info("错题归档完成：examId={}, 新增/累加 {} 题, 答对 {} 题", record.getId(), wrong, mastered);
    }

    @Override
    public List<WrongQuestionVO> queryMyWrong(Integer mastered) {
        Long userId = getUserId();
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .orderByDesc(WrongQuestion::getLastWrongTime);
        if (mastered != null) {
            wrapper.eq(WrongQuestion::getMastered, mastered);
        }
        List<WrongQuestion> list = wrongQuestionMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = list.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, QuestionDTO> questionMap = questionService.queryQuestionByIds(ids).stream()
                .collect(Collectors.toMap(QuestionDTO::getId, q -> q, (a, b) -> a));
        return list.stream()
                .map(wq -> toVO(wq, questionMap.get(wq.getQuestionId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<PointStatVO> queryPointStat() {
        return wrongQuestionMapper.statByPoint(getUserId());
    }

    @Override
    public void markMastered(Long questionId) {
        Long userId = getUserId();
        wrongQuestionMapper.markMastered(userId, questionId);
    }

    @Override
    public void remove(Long questionId) {
        Long userId = getUserId();
        wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId));
    }

    private Long getUserId() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        return userId;
    }

    private WrongQuestionVO toVO(WrongQuestion wq, QuestionDTO question) {
        if (question == null) {
            return null;
        }
        return new WrongQuestionVO()
                .setQuestionId(question.getId())
                .setName(question.getName())
                .setType(question.getType())
                .setOptions(question.getOptions())
                .setAnswer(question.getAnswer())
                .setAnalysis(question.getAnalysis())
                .setLastAnswer(wq.getLastAnswer())
                .setWrongCount(wq.getWrongCount())
                .setLastWrongTime(wq.getLastWrongTime())
                .setSectionName(wq.getSectionName())
                .setMastered(wq.getMastered() != null && wq.getMastered() == 1);
    }
}
